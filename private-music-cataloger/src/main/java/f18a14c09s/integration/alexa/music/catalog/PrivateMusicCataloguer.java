package f18a14c09s.integration.alexa.music.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.MusicAlbumCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicGroupCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.S3MediaFile;
import f18a14c09s.integration.alexa.music.catalog.data.S3MediaFolder;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSource;
import f18a14c09s.integration.alexa.music.data.ArtSourceSize;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.json.JSONAdapter;
import f18a14c09s.integration.mp3.ImageMetadata;
import f18a14c09s.integration.mp3.Mp3Adapter;
import f18a14c09s.integration.mp3.Mp3Folder;
import f18a14c09s.integration.mp3.TrackMetadata;
import f18a14c09s.util.CsvFileWriter;
import lombok.Getter;
import lombok.Setter;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static f18a14c09s.integration.alexa.music.catalog.CataloguerReportFactory.toReportRow;
import static f18a14c09s.util.CollectionUtil.asArrayList;

class PrivateMusicCataloguer {
    private String sourceS3BucketName;
    private String baseUrl;
    private String imageBaseUrl;
    private boolean live;
    private DynamoDBCatalogDAO catalogDAO;
    private EntityFactory entityFactory;
    private Mp3Adapter mp3Adapter;
    private JSONAdapter jsonAdapter;
    private MessageDigest sha256Digester;
    private Base64.Encoder base64Encoder;
    private S3Client s3Client = S3Client.create();
    private S3MusicDAO s3MusicDao;
    private CsvFileWriter reportWriter;
    private Locale en_US;
    private Art defaultArt;
    private PreexistingCatalog preexistingCatalog = new PreexistingCatalog();

    @Getter
    @Setter
    private static class PreexistingCatalog {
        private Map<String, Artist> artistsById;
        private Map<String, Album> albumsById;
        private Map<String, Track> tracksById;
        private Map<String, List<Artist>> artistsByName;
        private Map<List<String>, List<Album>> albumsByArtistAndName;
        private Map<List<String>, List<Track>> tracksByArtistAlbumAndName;

        public Artist findArtistById(String artistId) {
            return artistsById.get(artistId);
        }

        public Album findAlbumById(String albumId) {
            return albumsById.get(albumId);
        }

        public Track findTrackById(String trackId) {
            return tracksById.get(trackId);
        }

        public Collection<Artist> matchArtistsByName(Artist artist) {
            return Optional.ofNullable(artist.getNames())
                    .orElse(List.of())
                    .stream()
                    .flatMap(name -> artistsByName.get(name.getValue()).stream())
                    .collect(Collectors.toMap(BaseEntity::getId, Function.identity(), (lhs, rhs) -> lhs))
                    .values();
        }

        public Collection<Album> matchAlbumsByArtistAndName(Album album) {
            List<List<String>>
                    artistsAndNames =
                    album.getNames()
                            .stream()
                            .map(EntityName::getValue)
                            .flatMap(albumName -> album.getArtists()
                                    .stream()
                                    .flatMap(artistReference -> artistReference.getNames().stream())
                                    .map(EntityName::getValue)
                                    .map(artistName -> List.of(artistName, albumName)))
                            .collect(Collectors.toList());
            return artistsAndNames
                    .stream()
                    .flatMap(artistNameAlbumName -> albumsByArtistAndName.computeIfAbsent(artistNameAlbumName,
                                    key -> List.of())
                            .stream())
                    .collect(Collectors.toMap(BaseEntity::getId, Function.identity(), (lhs, rhs) -> lhs))
                    .values();
        }

        public Collection<Track> matchTracksByArtistAlbumAndName(Track track) {
            List<List<String>>
                    artistsAlbumsAndNames =
                    track.getNames()
                            .stream()
                            .map(EntityName::getValue)
                            .flatMap(trackName -> track.getArtists()
                                    .stream()
                                    .flatMap(artistReference -> artistReference.getNames().stream())
                                    .map(EntityName::getValue)
                                    .flatMap(artistName -> track.getAlbums()
                                            .stream()
                                            .flatMap(albumReference -> albumReference.getNames()
                                                    .stream()
                                                    .map(EntityName::getValue)
                                                    .map(albumName -> List.of(artistName, albumName, trackName)))))
                            .collect(Collectors.toList());
            return artistsAlbumsAndNames
                    .stream()
                    .flatMap(artistNameAlbumName -> tracksByArtistAlbumAndName.computeIfAbsent(artistNameAlbumName,
                                    key -> List.of())
                            .stream())
                    .collect(Collectors.toMap(BaseEntity::getId, Function.identity(), (lhs, rhs) -> lhs))
                    .values();
        }
    }

    public interface ExceptionalSupplier<T> {
        T get() throws Exception;

        static <T> T wrap(
                ExceptionalSupplier<T> supplier
        ) {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    PrivateMusicCataloguer(String sourceS3BucketName,
                           String sourceS3Prefix,
                           String baseUrl,
                           String imageBaseUrl,
                           String destStrStrDynamodbTableName,
                           String destStrNumDynamodbTableName,
                           boolean live) throws IOException, NoSuchAlgorithmException {
        this.sourceS3BucketName = sourceS3BucketName;
        this.baseUrl = baseUrl;
        this.imageBaseUrl = imageBaseUrl;
        this.live = live;
        this.mp3Adapter = new Mp3Adapter();
        this.jsonAdapter = new JSONAdapter();
        this.sha256Digester = MessageDigest.getInstance("SHA-256");
        this.base64Encoder = Base64.getEncoder();
        this.s3MusicDao = new S3MusicDAO(sourceS3BucketName, sourceS3Prefix);
        //
        this.defaultArt = defaultArtObject(imageBaseUrl);
        this.en_US = Locale.en_US();
        this.catalogDAO = new DynamoDBCatalogDAO(
                destStrStrDynamodbTableName,
                destStrNumDynamodbTableName
        );
        //
        loadPreexistingCatalog();
        //
        Map<EntityType, Map<List<String>, String>> entityIdsByTypeAndNaturalKey = new HashMap<>();
        for (Map.Entry<String, List<Artist>> artistNameArtists : preexistingCatalog.getArtistsByName().entrySet()) {
            if (artistNameArtists.getValue().size() > 1) {
                System.out.printf("More than one artist (%s total) with name \"%s\" found.%n",
                        artistNameArtists.getValue()
                                .size(), artistNameArtists.getKey());
                entityIdsByTypeAndNaturalKey.computeIfAbsent(EntityType.ARTIST, key -> new HashMap<>()).put(
                        List.of(artistNameArtists.getKey()), artistNameArtists.getValue().get(0).getId()
                );
            }
        }
        for (Map.Entry<List<String>, List<Album>> albumArtistNameAlbums : preexistingCatalog.getAlbumsByArtistAndName()
                .entrySet()) {
            if (albumArtistNameAlbums.getValue().size() > 1) {
                System.out.printf("More than one album (%s total) with (artist, name) \"%s\" found.%n",
                        albumArtistNameAlbums.getValue()
                                .size(), albumArtistNameAlbums.getKey());
                entityIdsByTypeAndNaturalKey.computeIfAbsent(EntityType.ALBUM, key -> new HashMap<>()).put(
                        albumArtistNameAlbums.getKey(), albumArtistNameAlbums.getValue().get(0).getId()
                );
            }
        }
        for (Map.Entry<List<String>, List<Track>> trackArtistAlbumNameTracks :
                preexistingCatalog.getTracksByArtistAlbumAndName()
                .entrySet()) {
            if (trackArtistAlbumNameTracks.getValue().size() > 1) {
                System.out.printf("More than one track (%s total) with (artist, album, name) \"%s\" found.%n",
                        trackArtistAlbumNameTracks.getValue()
                                .size(), trackArtistAlbumNameTracks.getKey());
                entityIdsByTypeAndNaturalKey.computeIfAbsent(EntityType.TRACK, key -> new HashMap<>()).put(
                        trackArtistAlbumNameTracks.getKey(), trackArtistAlbumNameTracks.getValue().get(0).getId()
                );
            }
        }
        this.entityFactory = new EntityFactory(en_US, entityIdsByTypeAndNaturalKey);
    }

    private void loadPreexistingCatalog() {
        List<Artist>
                artists =
                catalogDAO.listArtists()
                        .stream()
                        .filter(artist -> artist.getDeleted() == null || !artist.getDeleted())
                        .collect(Collectors.toList());
        List<Album>
                albums =
                catalogDAO.listAlbums()
                        .stream()
                        .filter(album -> album.getDeleted() == null || !album.getDeleted())
                        .collect(Collectors.toList());
        List<Track>
                tracks =
                catalogDAO.listTracks()
                        .stream()
                        .filter(track -> track.getDeleted() == null || !track.getDeleted())
                        .collect(Collectors.toList());
        //
        preexistingCatalog.setArtistsById(artists.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity())));
        preexistingCatalog.setArtistsByName(new HashMap<>());
        for (Artist artist : artists) {
            for (EntityName name : Optional.ofNullable(artist.getNames()).orElse(List.of())) {
                preexistingCatalog.getArtistsByName().computeIfAbsent(
                        name.getValue(), key -> new ArrayList<>()
                ).add(artist);
            }
        }
        //
        preexistingCatalog.setAlbumsById(albums.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity())));
        preexistingCatalog.setAlbumsByArtistAndName(new HashMap<>());
        for (Album album : albums) {
            for (EntityName albumName : Optional.ofNullable(album.getNames()).orElse(List.of())) {
                for (ArtistReference artistReference : Optional.ofNullable(album.getArtists()).orElse(List.of())) {
                    for (EntityName artistName : Optional.ofNullable(artistReference.getNames()).orElse(List.of())) {
                        List<String> artistAndName = List.of(artistName.getValue(), albumName.getValue());
                        preexistingCatalog.getAlbumsByArtistAndName().computeIfAbsent(
                                artistAndName, key -> new ArrayList<>()
                        ).add(album);
                    }
                }
            }
        }
        //
        preexistingCatalog.setTracksById(tracks.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity())));
        preexistingCatalog.setTracksByArtistAlbumAndName(new HashMap<>());
        for (Track track : tracks) {
            for (EntityName trackName : Optional.ofNullable(track.getNames()).orElse(List.of())) {
                for (AlbumReference albumReference : track.getAlbums()) {
                    for (EntityName albumName : Optional.ofNullable(albumReference.getNames()).orElse(List.of())) {
                        for (ArtistReference artistReference : Optional.ofNullable(track.getArtists())
                                .orElse(List.of())) {
                            for (EntityName artistName : Optional.ofNullable(artistReference.getNames())
                                    .orElse(List.of())) {
                                List<String>
                                        artistAlbumAndName =
                                        List.of(artistName.getValue(), albumName.getValue(), trackName.getValue());
                                preexistingCatalog.getTracksByArtistAlbumAndName().computeIfAbsent(
                                        artistAlbumAndName, key -> new ArrayList<>()
                                ).add(track);
                            }
                        }
                    }
                }
            }
        }
    }

    private CsvFileWriter newReportWriter() throws IOException {
        File outputFile = new File(
                String.format(
                        "Cataloguer Report %s.csv",
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                                ZonedDateTime.now()
                        ).replaceAll(":", "-")
                )
        );
        System.out.printf("Report will be written to %s.%n", outputFile.getAbsolutePath());
        return CsvFileWriter.from(
                outputFile,
                CataloguerReportFactory.DEFAULT_HEADER
        );
    }

    private synchronized void writeReportRow(List<String> row) {
        reportWriter.writeRow(row);
    }

    void catalogMusic() throws IOException {
        try (CsvFileWriter csvFileWriter = newReportWriter()) {
            csvFileWriter.writeHeader();
            this.reportWriter = csvFileWriter;
            //
            S3MediaFolder rootFolderReference = s3MusicDao.recurseMediaFolder();
            Mp3Folder rootMp3Folder = catalogMusicFolder(rootFolderReference, 0);
            printFolderSummary(rootMp3Folder);
            Map<String, Artist> artists = catalogArtists(rootMp3Folder);
            Map<String, String> artistIdToName = artists.entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
            Map<String, ArtistReference> artistReferences = artists.values().stream().collect(Collectors.toMap(
                    artist -> artistIdToName.get(artist.getId()),
                    artist -> ExceptionalSupplier.wrap(
                            () -> jsonAdapter.readValue(
                                    jsonAdapter.writeValueAsString(artist),
                                    ArtistReference.class
                            )
                    )
            ));
            Map<ArrayList<String>, Album> albums = catalogAlbums(rootMp3Folder, artistReferences);
            Map<String, ArrayList<String>> albumIdToName = albums.entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
            Map<ArrayList<String>, AlbumReference> albumReferences = albums.values().stream().collect(Collectors.toMap(
                    album -> albumIdToName.get(album.getId()),
                    album -> ExceptionalSupplier.wrap(
                            () -> jsonAdapter.readValue(
                                    jsonAdapter.writeValueAsString(album),
                                    AlbumReference.class
                            )
                    )
            ));
            deduplicateAndCatalogTracks(rootMp3Folder, artistReferences, albumReferences);
        } finally {
            this.reportWriter = null;
        }
    }

    private void printFolderSummary(Mp3Folder rootMp3Folder) {
        System.out.printf(
                "Root Folder:%n\tHas Mp3s: %s" + "%n\tLikely Artist Folders: %s%n\tNot Likely Artist Folders: %s" +
                        "%n\tLikely Album Folders: %s%n\tNot Likely Album Folders: %s" +
                        "%n\tNon-Unique Artist Names:%s%n" + "%n\tNon-Unique Album Names:%s%n",
                !rootMp3Folder.getMp3s().isEmpty(),
                rootMp3Folder.getChildren().stream().filter(Mp3Folder::isLikelyArtistFolder).count(),
                rootMp3Folder.getChildren()
                        .stream()
                        .filter(((Predicate<Mp3Folder>) Mp3Folder::isLikelyArtistFolder).negate())
                        .count(),
                rootMp3Folder.getChildren().stream().filter(Mp3Folder::isLikelyAlbumFolder).count(),
                rootMp3Folder.getChildren()
                        .stream()
                        .filter(((Predicate<Mp3Folder>) Mp3Folder::isLikelyAlbumFolder).negate())
                        .count(),
                rootMp3Folder.getChildren()
                        .stream()
                        .filter(child -> child.isLikelyArtistFolder() && child.getUniqueArtistNames().size() > 1)
                        .map(child -> String.format("%n\t\tNon-Unique Names (%s total): %s",
                                child.getUniqueArtistNames().size(),
                                child.getUniqueArtistNames()))
                        .collect(Collectors.joining()),
                rootMp3Folder.getChildren()
                        .stream()
                        .flatMap(child -> Stream.concat(Stream.of(child),
                                child.getChildren() == null ? Stream.empty() : child.getChildren().stream()))
                        .filter(child -> child.isLikelyAlbumFolder() && child.getUniqueAlbumNames().size() > 1)
                        .map(child -> String.format("%n\t\tNon-Unique Names (%s total): %s",
                                child.getUniqueAlbumNames().size(),
                                child.getUniqueAlbumNames()))
                        .collect(Collectors.joining()));
    }

    private Map<String, Artist> catalogArtists(Mp3Folder rootMp3Folder) {
        Map<String, Artist> artists = new HashMap<>();
        List<Mp3Folder> folders = asArrayList(rootMp3Folder);
        while (!folders.isEmpty()) {
            Mp3Folder folder = folders.remove(0);
            folders.addAll(folder.getChildren());
            folder.getUniqueArtistNames().forEach(artistName -> {
                if (!artists.containsKey(artistName)) {
                    artists.put(artistName, entityFactory.newArtistEntity(artistName,
                            Optional.ofNullable(folder.getArt()).orElse(defaultArt)));
                }
            });
        }
        MusicGroupCatalog catalog = new MusicGroupCatalog();
        catalog.setEntities(new ArrayList<>(artists.values()));
        catalog.setLocales(asArrayList(en_US));
        for (Artist artist : artists.values()) {
            if (live) {
                catalogDAO.save(artist);
            }
            writeReportRow(toReportRow(artist, preexistingCatalog.findArtistById(artist.getId()),
                    preexistingCatalog.matchArtistsByName(artist)));
        }
        return artists;
    }

    private Map<ArrayList<String>, Album> catalogAlbums(Mp3Folder rootMp3Folder,
                                                        Map<String, ArtistReference> artists) throws IOException {
        Map<ArrayList<String>, Album> albums = new HashMap<>();
        List<Mp3Folder> folders = asArrayList(rootMp3Folder);
        while (!folders.isEmpty()) {
            Mp3Folder folder = folders.remove(0);
            folders.addAll(folder.getChildren());
            folder.getUniqueAlbumNames().forEach(albumKey -> {
                ArrayList<String> keyAsList = new ArrayList<>(albumKey.toList());
                if (!albums.containsKey(keyAsList)) {
                    albums.put(keyAsList, entityFactory.newAlbumEntity(albumKey.getAlbumName(),
                            artists.get(albumKey.getArtistName()),
                            Optional.ofNullable(folder.getArt()).orElse(defaultArt)));
                }
            });
        }
        MusicAlbumCatalog catalog = new MusicAlbumCatalog();
        catalog.setEntities(new ArrayList<>(albums.values()));
        catalog.setLocales(asArrayList(en_US));
        for (Album album : albums.values()) {
            if (live) {
                catalogDAO.save(album);
            }
            writeReportRow(toReportRow(album, preexistingCatalog.findAlbumById(album.getId()),
                    preexistingCatalog.matchAlbumsByArtistAndName(album)));
        }
        return albums;
    }

    private Track newTrackEntity(
            TrackMetadata metadata,
            Mp3Folder folder,
            Map<String, ArtistReference> artists,
            Map<ArrayList<String>, AlbumReference> albums
    ) throws JsonProcessingException {
        Set<String> trackArtistNames = Optional.of(metadata.getDistinctArtistNames())
                .map(Map::keySet)
                .filter(Predicate.not(Set::isEmpty))
                .orElse(Set.of("Unknown"));
        List<ArtistReference> trackArtists = trackArtistNames.stream()
                .map(artists::get)
                .collect(Collectors.toList());
        System.out.println(jsonAdapter.writeValueAsString(metadata));
        AlbumReference trackAlbum = trackArtistNames.stream().filter(Objects::nonNull).map(
                trackArtistName -> asArrayList(
                        trackArtistName,
                        metadata.getAlbum()
                )
        ).map(albums::get).filter(Objects::nonNull).findAny().get();
        return entityFactory.newTrackEntity(metadata,
                buildUrl(metadata.getFilePath()),
                trackArtists,
                trackAlbum,
                Optional.ofNullable(folder.getArt()).orElse(defaultArt));
    }

    public class CatalogPersistence {
        @Getter
        private List<Future<Track>> tasks = new ArrayList<>();
        @Getter
        private ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(20);

        public void saveTracks(List<Track> tracks) throws InterruptedException {
            tasks.addAll(
                    threadPoolExecutor.invokeAll(
                            tracks.stream().<Callable<Track>>map(
                                    track -> () -> {
                                        if (live) {
                                            try {
                                                catalogDAO.save(track);
                                            } catch (RuntimeException e) {
                                                System.out.printf(
                                                        "Failure:%n\t%s%n",
                                                        jsonAdapter.writeValueAsString(track)
                                                );
                                                throw e;
                                            }
                                        }
                                        writeReportRow(toReportRow(track,
                                                preexistingCatalog.findTrackById(track.getId()),
                                                preexistingCatalog.matchTracksByArtistAlbumAndName(track)));
                                        return track;
                                    }
                            ).collect(Collectors.toList())
                    )
            );
        }

        public void associateTracksToArtists(Map<String, List<Track>> childTracksByArtistId) throws InterruptedException {
            tasks.addAll(
                    threadPoolExecutor.invokeAll(
                            childTracksByArtistId.entrySet().stream().<Callable<Track>>map(
                                    artistIdChildTracks -> () -> {
                                        if (live) {
                                            catalogDAO.saveChildTrackAssociations(
                                                    Artist.class,
                                                    artistIdChildTracks.getKey(),
                                                    artistIdChildTracks.getValue().stream().sorted(
                                                            Comparator.<Track, Long>comparing(
                                                                    track -> track.getAlbums()
                                                                            .stream()
                                                                            .map(
                                                                                    albumReference -> Optional.ofNullable(albumReference.getNaturalOrder())
                                                                                            .orElse(0L)
                                                                            )
                                                                            .findAny()
                                                                            .orElse(0L)
                                                            ).thenComparing(
                                                                    track -> track.getAlbums().stream().flatMap(
                                                                            albumReference -> albumReference.getNames()
                                                                                    .stream()
                                                                    ).map(EntityName::getValue).findAny().orElse("")
                                                            ).thenComparing(
                                                                    track -> Optional.ofNullable(track.getNaturalOrder())
                                                                            .orElse(0L)
                                                            )
                                                    ).collect(Collectors.toList())
                                            );
                                        }
                                        return null;
                                    }
                            ).collect(Collectors.toList())
                    )
            );
        }

        public void associateTracksToAlbums(Map<String, List<Track>> childTracksByAlbumId) throws InterruptedException {
            tasks.addAll(
                    threadPoolExecutor.invokeAll(
                            childTracksByAlbumId.entrySet().stream().<Callable<Track>>map(
                                    albumIdChildTracks -> () -> {
                                        if (live) {
                                            catalogDAO.saveChildTrackAssociations(
                                                    Album.class,
                                                    albumIdChildTracks.getKey(),
                                                    albumIdChildTracks.getValue().stream().sorted(
                                                            Comparator.comparing(
                                                                    track -> Optional.ofNullable(track.getNaturalOrder())
                                                                            .orElse(0L)
                                                            )
                                                    ).collect(Collectors.toList())
                                            );
                                        }
                                        return null;
                                    }
                            ).collect(Collectors.toList())
                    )
            );
        }
    }

    private void deduplicateAndCatalogTracks(Mp3Folder rootMp3Folder,
                                             Map<String, ArtistReference> artists,
                                             Map<ArrayList<String>, AlbumReference> albums) throws IOException {
        List<Track> tracks = new ArrayList<>();
        Map<String, List<Track>> childTracksByArtistId = new HashMap<>();
        Map<String, List<Track>> childTracksByAlbumId = new HashMap<>();
        List<Mp3Folder> mp3Folders = asArrayList(rootMp3Folder);
        Map<String, String> trackUrlsById = new HashMap<>();

        while (!mp3Folders.isEmpty()) {
            Mp3Folder folder = mp3Folders.remove(0);
            mp3Folders.addAll(folder.getChildren());
            for (TrackMetadata metadata : folder.getMp3s()) {
                Track trackEntity = newTrackEntity(
                        metadata, folder, artists, albums
                );
                if (trackUrlsById.containsKey(trackEntity.getId())) {
                    System.out.printf(
                            "Track appears to be a duplicate:%n\tEntity ID: %s%n\tTrack URL: %s%n\tPotential " +
                                    "Duplicate URL: %s%n",
                            trackEntity.getId(),
                            trackUrlsById.get(trackEntity.getId()),
                            trackEntity.getUrl()
                    );
                } else {
                    tracks.add(trackEntity);
                    trackUrlsById.put(trackEntity.getId(), trackEntity.getUrl());
                    for (ArtistReference artistReference : trackEntity.getArtists()) {
                        System.out.println(
                                jsonAdapter.writeValueAsString(artistReference)
                        );
                        childTracksByArtistId.computeIfAbsent(
                                artistReference.getId(),
                                artistId -> new ArrayList<>()
                        ).add(trackEntity);
                    }
                    for (AlbumReference albumReference : trackEntity.getAlbums()) {
                        childTracksByAlbumId.computeIfAbsent(
                                albumReference.getId(),
                                artistId -> new ArrayList<>()
                        ).add(trackEntity);
                    }
                }
            }
        }
        saveTracksToCatalog(tracks, childTracksByArtistId, childTracksByAlbumId);
    }

    private void saveTracksToCatalog(List<Track> tracks, Map<String, List<Track>> childTracksByArtistId, Map<String,
            List<Track>> childTracksByAlbumId) throws IOException {
        CatalogPersistence persistence = new CatalogPersistence();

        try {
            persistence.saveTracks(tracks);
            persistence.associateTracksToArtists(childTracksByArtistId);
            persistence.associateTracksToAlbums(childTracksByAlbumId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            persistence.getThreadPoolExecutor().shutdown();
        }

        for (Future<Track> task : persistence.getTasks()) {
            try {
                Track success = task.get();
                System.out.printf(
                        "Success:%n\t%s%n",
                        jsonAdapter.writeValueAsString(success)
                );
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Mp3Folder catalogMusicFolder(S3MediaFolder folderReference, int level) {
        List<Mp3Folder> children = new ArrayList<>();
        List<TrackMetadata> mp3s = catalogMusicTracks(folderReference.getMusicFiles());
        for (S3MediaFolder subfolderReference : folderReference.getSubfolders()) {
            Mp3Folder child = catalogMusicFolder(subfolderReference, level + 1);
            children.add(child);
            System.out.printf(
                    "Found %s MP3s under S3 prefix %s.%n",
                    child.recurseMp3s().count(),
                    subfolderReference.getS3Key()
            );
        }
        return new Mp3Folder(mp3s, catalogAlbumArt(folderReference.getArtFiles()), level, children);
    }

    private TrackMetadata parseTrackMetadata(S3MediaFile musicFile) {
        TrackMetadata trackMetadata = (
                live ?
                        catalogDAO.findTrackMetadata(musicFile.getPath()) :
                        null
        );
        if (trackMetadata != null) {
            System.out.printf(
                    "Track metadata found in DynamoDB for S3 object %s.%n",
                    musicFile.getS3Key()
            );
            return trackMetadata;
        }
        System.out.printf(
                "Retrieving S3 object %s.%n",
                musicFile.getS3Key()
        );
        try (InputStream s3InputStream = s3Client.getObject(
                GetObjectRequest.builder().bucket(
                        sourceS3BucketName
                ).key(
                        musicFile.getS3Key()
                ).build()
        )) {
            TrackMetadata track = mp3Adapter.parseMetadata(
                    s3InputStream
            );
            track.setFilePath(musicFile.getPath());
            track.setAlbum(Optional.ofNullable(track.getAlbum()).orElse("Unknown"));
            track.setTitle(Optional.ofNullable(track.getTitle()).orElse("Unknown"));
            return track;
        } catch (IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException e) {
            throw new RuntimeException(String.format("Failure retrieving or parsing %s.", musicFile.getS3Key()), e);
        }
    }

    private List<TrackMetadata> catalogMusicTracks(List<S3MediaFile> musicFiles) {
        List<TrackMetadata> trackMetadataList = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            List<Future<TrackMetadata>> tasks = executor.invokeAll(musicFiles.stream()
                    .<Callable<TrackMetadata>>map(s3Object -> (() -> parseTrackMetadata(s3Object)))
                    .collect(Collectors.toList()));
            for (Future<TrackMetadata> task : tasks) {
                trackMetadataList.add(task.get());
            }
            List<Future<?>> persistenceTasks = new ArrayList<>();
            for (TrackMetadata trackMetadata : trackMetadataList) {
                persistenceTasks.add(executor.submit(
                        () -> {
                            if (live) {
                                catalogDAO.save(trackMetadata);
                            }
                            writeReportRow(toReportRow(trackMetadata));
                        }
                ));
            }
            for (Future<?> persistenceTask : persistenceTasks) {
                persistenceTask.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
        return trackMetadataList;
    }

    private List<ImageMetadata> catalogAlbumArt(List<S3MediaFile> albumArtFiles) {
        return albumArtFiles.stream().map(jpg -> {
            try (InputStream s3InputStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(sourceS3BucketName)
                    .key(jpg.getS3Key())
                    .build())) {
                return newImageMetadata(s3InputStream, buildUrl(jpg.getPath()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to access image " + jpg.getS3Key() + ".", e);
            }
        }).distinct().collect(Collectors.toList());
    }

    private ImageMetadata newImageMetadata(InputStream fis, String url) throws IOException {
        Long width, height;
        String sha256Hash;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                for (int b = bis.read(); b >= 0; b = bis.read()) {
                    baos.write(b);
                }
            }
            baos.flush();
            sha256Hash = base64Encoder.encodeToString(sha256Digester.digest(baos.toByteArray()));
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                BufferedImage image = ImageIO.read(bais);
                if (image == null) {
                    width = null;
                    height = null;
                } else {
                    width = (long) image.getWidth();
                    height = (long) image.getHeight();
                }
            }
        }
        return new ImageMetadata(url, sha256Hash, width, height);
    }

    private String buildUrl(String relativePath) {
        return String.format("%s/%s",
                baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.lastIndexOf("/")) : baseUrl,
                relativePath);
    }

    private Art defaultArtObject(String baseUrl) throws IOException {
        Art retval = new Art();
        final String musicIconRelativePath = "minduka/music-icon";
        retval.setContentDescription("Music icon (A. Minduka, https://openclipart.org/detail/27648/music-icon).");
        List<ImageMetadata> imageMetadata = new ArrayList<>();
        for (String png : new String[]{"small", "medium", "large"}) {
            String relativePath = String.format("%s/%s.png", musicIconRelativePath, png);
            try (InputStream inputStream = getClass().getResourceAsStream(String.format("/images/%s", relativePath))) {
                String url = String.format("%s/%s", baseUrl, relativePath);
                ImageMetadata img = newImageMetadata(inputStream, url);
                img.setArtSourceSize(Optional.of(ArtSourceSize.valueOf(png.toUpperCase())).get());
                imageMetadata.add(img);
            }
        }
        retval.setSources(ImageMetadata.getExactlyOnePerSize(imageMetadata)
                .entrySet()
                .stream()
                .map(entry -> new ArtSource(entry.getValue().getUrl(),
                        entry.getKey(),
                        entry.getValue().getWidth(),
                        entry.getValue().getHeight()))
                .collect(Collectors.toList()));
        return retval;
    }
}
