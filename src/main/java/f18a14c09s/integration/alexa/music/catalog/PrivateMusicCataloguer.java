package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicAlbumCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicGroupCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicRecordingCatalog;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSource;
import f18a14c09s.integration.alexa.music.data.ArtSourceSize;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.hibernate.Hbm2DdlAuto;
import f18a14c09s.integration.json.JSONAdapter;
import f18a14c09s.integration.mp3.ImageMetadata;
import f18a14c09s.integration.mp3.Mp3Adapter;
import f18a14c09s.integration.mp3.Mp3Folder;
import f18a14c09s.integration.mp3.TrackMetadata;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.springframework.web.util.UriUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static f18a14c09s.util.CollectionUtil.asArrayList;

class PrivateMusicCataloguer {
    public static final String MP3EXT = ".mp3";
    public static final String JPGEXT = ".jpg";
    private boolean reset = true;
    private boolean writeToDisk;
    private boolean writeToDb;
    private File srcDir;
    private File destDir;
    private String baseUrl;
    private CatalogDAO dao;
    private Mp3Adapter mp3Adapter = new Mp3Adapter();
    private Mp3ToAlexaCatalog mp3ToAlexaCatalog = new Mp3ToAlexaCatalog();
    private JSONAdapter jsonAdapter = new JSONAdapter();
    private Locale en_US = Locale.en_US();
    private Art defaultArt;
    private Map<EntityType, Map<List<String>, String>> entityIdsByTypeAndNaturalKey;

    PrivateMusicCataloguer(File srcDir,
                           File destDir,
                           String baseUrl,
                           boolean reset,
                           String imageBaseUrl,
                           boolean writeToDb) throws IOException {
        this.srcDir = srcDir;
        this.destDir = destDir;
        this.baseUrl = baseUrl;
        this.reset = reset;
        this.writeToDisk = (destDir != null);
        this.writeToDb = writeToDb;
        this.defaultArt = defaultArtObject(imageBaseUrl);
        if (writeToDb) {
            this.dao = new CatalogDAO(Hbm2DdlAuto.create);
            this.entityIdsByTypeAndNaturalKey = dao.getCataloguedEntityIdsByTypeAndNaturalKey();
        }
    }

    void catalogMusic() throws IOException, NoSuchAlgorithmException {
        if (writeToDb) {
            dao.save(en_US);
            dao.save(defaultArt);
            dao.commit();
            en_US = dao.findLocale(en_US.getCountry(), en_US.getLanguage());
        }
//        System.out.printf("Locale %s-%s has ID %s.%n", en_US.getLanguage(), en_US.getCountry(), en_US.getId());
        Mp3Folder rootMp3Folder = collectTrackInfoRecursively(srcDir, 0);
//        printFolderSummary(rootMp3Folder);
//        List<Mp3Folder> mp3Folders = new ArrayList<>();
//        mp3FoldersAddAllRecursive(rootMp3Folder, mp3Folders);
//        mp3Folders.forEach(folder -> folder.getMp3s().forEach(track -> {
//            track.setAlbum(Optional.ofNullable(track.getAlbum()).filter(s -> !s.trim().isEmpty()).orElse("Unknown"));
//            track.setAuthor(Optional.ofNullable(track.getAuthor()).filter(s -> !s.trim().isEmpty()).orElse("Unknown"));
//        }));
        if (writeToDb) {
            Map<String, String> artistIdToName = catalogArtists(rootMp3Folder).entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
            Map<String, ArtistReference> artists = dao.findAllArtistReferences()
                    .stream()
                    .collect(Collectors.toMap(artist -> artistIdToName.get(artist.getId()), UnaryOperator.identity()));
            Map<String, ArrayList<String>> albumIdToName = catalogAlbums(rootMp3Folder, artists).entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
            Map<ArrayList<String>, AlbumReference> albums = dao.findAllAlbumReferences()
                    .stream()
                    .collect(Collectors.toMap(album -> albumIdToName.get(album.getId()),
                            UnaryOperator.identity(),
                            (lhs, rhs) -> {
                                if (Objects.equals(lhs.getId(), rhs.getId()) ||
                                        Objects.equals(albumIdToName.get(lhs.getId()),
                                                albumIdToName.get(rhs.getId()))) {
                                    System.out.printf("Duplicate:%n\tLHS: %s%n\t\t%s%n\tRHS: %s%n\t\t%s%n",
                                            lhs,
                                            albumIdToName.get(lhs.getId()),
                                            rhs,
                                            albumIdToName.get(rhs.getId()));
                                }
                                return lhs;
                            }));
            List<SongDTO> tracks = new ArrayList<>();
//            mp3Folders.stream()
//                    .flatMap(folder -> folder.getMp3s()
//                            .stream()
//                            .map(mp3 -> new SongDTO(mp3, Optional.ofNullable(folder.getArt()).orElse(defaultArt))))
//                    .collect(Collectors.toList());
            catalogTracks(rootMp3Folder, artists, albums);
            printDbSummary();
            dao.close(true);
        }
    }

    private void mp3FoldersAddAllRecursive(Mp3Folder folder, List<Mp3Folder> mp3Folders) {
        if (folder.getMp3s() != null && !folder.getMp3s().isEmpty()) {
            mp3Folders.add(folder);
        }
        folder.getChildren().forEach(child -> mp3FoldersAddAllRecursive(child, mp3Folders));
    }

//    private void printFolderSummary(Mp3Folder rootMp3Folder) {
//        System.out.printf(
//                "Root Folder:%n\tHas Mp3s: %s" + "%n\tLikely Artist Folders: %s%n\tNot Likely Artist Folders: %s" +
//                        "%n\tLikely Album Folders: %s%n\tNot Likely Album Folders: %s" +
//                        "%n\tNon-Unique Artist Names:%s%n" + "%n\tNon-Unique Album Names:%s%n",
//                !rootMp3Folder.getMp3s().isEmpty(),
//                rootMp3Folder.getChildren().stream().filter(Mp3Folder::isLikelyArtistFolder).count(),
//                rootMp3Folder.getChildren()
//                        .stream()
//                        .filter(((Predicate<Mp3Folder>) Mp3Folder::isLikelyArtistFolder).negate())
//                        .count(),
//                rootMp3Folder.getChildren().stream().filter(Mp3Folder::isLikelyAlbumFolder).count(),
//                rootMp3Folder.getChildren()
//                        .stream()
//                        .filter(((Predicate<Mp3Folder>) Mp3Folder::isLikelyAlbumFolder).negate())
//                        .count(),
//                rootMp3Folder.getChildren()
//                        .stream()
//                        .filter(child -> child.isLikelyArtistFolder() && child.getUniqueArtistNames().size() != 1)
//                        .map(child -> String.format("%n\t\tNon-Unique Names (%s total): %s",
//                                child.getUniqueArtistNames().size(),
//                                child.getUniqueArtistNames()))
//                        .collect(Collectors.joining()),
//                rootMp3Folder.getChildren()
//                        .stream()
//                        .flatMap(child -> Stream.concat(Stream.of(child),
//                                child.getChildren() == null ? Stream.empty() : child.getChildren().stream()))
//                        .filter(child -> child.isLikelyAlbumFolder() && child.getUniqueAlbumNames().size() != 1)
//                        .map(child -> String.format("%n\t\tNon-Unique Names (%s total): %s",
//                                child.getUniqueAlbumNames().size(),
//                                child.getUniqueAlbumNames()))
//                        .collect(Collectors.joining()));
//    }

    private void printDbSummary() {
        for (Class<?> clazz : asArrayList(Locale.class,
                MusicGroupCatalog.class,
                MusicAlbumCatalog.class,
                MusicRecordingCatalog.class,
                Artist.class,
                ArtistReference.class,
                Album.class,
                AlbumReference.class,
                Track.class,
                EntityName.class,
                AlternateNames.class,
                Popularity.class,
                PopularityOverride.class,
                Art.class,
                ArtSource.class)) {
            System.out.printf("Total %s objects: %s%n", clazz.getSimpleName(), dao.count(clazz));
        }
    }

    private void catalogTracks(List<SongDTO> trackMetadataAndAlbumArt,
                               Map<String, ArtistReference> artists,
                               Map<ArrayList<String>, AlbumReference> albums) throws IOException {
        List<Track> tracks = trackMetadataAndAlbumArt.stream().map(trackAndArt -> {
            TrackMetadata metadata = trackAndArt.getTrack();
            Track track = mp3ToAlexaCatalog.mp3ToTrackEntity(metadata);
            track.setArtists(asArrayList(artists.get(metadata.getAuthor())));
            track.setAlbums(asArrayList(albums.get(asArrayList(metadata.getAuthor(), metadata.getAlbum()))));
            track.setLocales(asArrayList(en_US));
            track.setUrl(buildUrl(metadata.getFilePath()));
            track.setArt(Optional.ofNullable(trackAndArt.getAlbumArt()).orElse(defaultArt));
//
            track.setId(Optional.of(entityIdsByTypeAndNaturalKey.get(EntityType.TRACK)
                    .get(Collections.singletonList(track.getUrl()))).get());
            return track;
        }).collect(Collectors.toList());
        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
        trackCatalog.setEntities(tracks);
        trackCatalog.setLocales(asArrayList(en_US));
        writeToDisk(trackCatalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-tracks.json"));
        dao.save(trackCatalog);
    }

    private Map<ArrayList<String>, Album> catalogAlbums(List<Mp3Folder> mp3Folders,
                                                        Map<String, ArtistReference> artists) throws IOException {
        List<SongDTO> tracks = mp3Folders.stream()
                .flatMap(folder -> folder.getMp3s()
                        .stream()
                        .map(mp3 -> new SongDTO(mp3, Optional.ofNullable(folder.getArt()).orElse(defaultArt))))
                .collect(Collectors.toList());
        Map<List<String>, List<Art>> artByArtistNameAlbumName = tracks.stream()
                .collect(Collectors.groupingBy(track -> asArrayList(track.getTrack().getAuthor(),
                        track.getTrack().getAlbum()),
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream()
                                        .map(SongDTO::getAlbumArt)
                                        .distinct()
                                        .collect(Collectors.toList()))));
        Map<ArrayList<String>, Album> albums = tracks.stream()
                .map(SongDTO::getTrack)
                .map(track -> asArrayList(track.getAuthor(), track.getAlbum()))
                .distinct()
                .collect(Collectors.toMap(UnaryOperator.identity(), artistAlbum -> {
                    Album album = newAlbumEntity(artistAlbum.get(1));
                    album.setId(Optional.of(entityIdsByTypeAndNaturalKey.get(EntityType.ALBUM).get(artistAlbum)).get());
                    album.setArtists(asArrayList(artists.get(artistAlbum.get(0))));
                    album.setArt(Optional.ofNullable(artByArtistNameAlbumName.get(artistAlbum))
                            .filter(list -> !list.isEmpty())
                            .map(list -> list.get(0))
                            .orElse(defaultArt));
                    return album;
                }));
        MusicAlbumCatalog catalog = new MusicAlbumCatalog();
        catalog.setEntities(new ArrayList<>(albums.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-albums.json"));
        dao.save(catalog);
        return albums;
    }

    private Map<String, Artist> catalogArtists(List<Mp3Folder> mp3Folders) throws IOException {
        List<SongDTO> tracks = mp3Folders.stream()
                .flatMap(folder -> folder.getMp3s()
                        .stream()
                        .map(mp3 -> new SongDTO(mp3, Optional.ofNullable(folder.getArt()).orElse(defaultArt))))
                .collect(Collectors.toList());
        Map<String, List<Art>> artistNameToArt = tracks.stream()
                .collect(Collectors.groupingBy(track -> track.getTrack().getAuthor(),
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream()
                                        .map(SongDTO::getAlbumArt)
                                        .distinct()
                                        .collect(Collectors.toList()))));
        Map<String, Artist> artists = tracks.stream()
                .map(SongDTO::getTrack)
                .map(TrackMetadata::getAuthor)
                .distinct()
                .collect(Collectors.toMap(UnaryOperator.identity(), artistName -> {
                    Artist artist = newArtistEntity(artistName);
                    artist.setArt(Optional.ofNullable(artistNameToArt.get(artistName))
                            .filter(list -> !list.isEmpty())
                            .map(list -> list.get(0))
                            .orElse(defaultArt));
                    return artist;
                }));
        MusicGroupCatalog catalog = new MusicGroupCatalog();
        artists.values()
                .forEach(artist -> artist.setArt(artistNameToArt.get(artist.getNames().get(0).getValue()).get(0)));
        catalog.setEntities(new ArrayList<>(artists.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-artists.json"));
        dao.save(catalog);
        return artists;
    }

    private Map<String, Artist> catalogArtists(Mp3Folder rootMp3Folder) throws IOException {
        Map<String, Artist> artists = new HashMap<>();
        List<Mp3Folder> folders = asArrayList(rootMp3Folder);
        while (!folders.isEmpty()) {
            Mp3Folder folder = folders.remove(0);
            folders.addAll(folder.getChildren());
            folder.getUniqueArtistNames().forEach(artistName -> {
                if (!artists.containsKey(artistName)) {
                    Artist artist = newArtistEntity(artistName);
                    artist.setArt(Optional.ofNullable(folder.getArt()).orElse(defaultArt));
                    artists.put(artistName, artist);
                }
            });
        }
        MusicGroupCatalog catalog = new MusicGroupCatalog();
        catalog.setEntities(new ArrayList<>(artists.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-artists.json"));
        dao.save(catalog);
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
                    Album album = newAlbumEntity(albumKey.getAlbumName());
                    album.setId(Optional.of(entityIdsByTypeAndNaturalKey.get(EntityType.ALBUM).get(keyAsList)).get());
                    album.setArtists(asArrayList(artists.get(albumKey.getArtistName())));
                    album.setArt(Optional.ofNullable(folder.getArt()).orElse(defaultArt));
                    albums.put(keyAsList, album);
                }
            });
        }
        MusicAlbumCatalog catalog = new MusicAlbumCatalog();
        catalog.setEntities(new ArrayList<>(albums.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-albums.json"));
        dao.save(catalog);
        return albums;
    }

    private void catalogTracks(Mp3Folder rootMp3Folder,
                               Map<String, ArtistReference> artists,
                               Map<ArrayList<String>, AlbumReference> albums) throws IOException {
        List<Track> tracks = new ArrayList<>();
        List<Mp3Folder> mp3Folders = asArrayList(rootMp3Folder);
        while (!mp3Folders.isEmpty()) {
            Mp3Folder folder = mp3Folders.remove(0);
            mp3Folders.addAll(folder.getChildren());
            folder.getMp3s().forEach(metadata -> {
                Track track = mp3ToAlexaCatalog.mp3ToTrackEntity(metadata);
                track.setArtists(asArrayList(artists.get(metadata.getAuthor())));
                track.setAlbums(asArrayList(albums.get(asArrayList(metadata.getAuthor(), metadata.getAlbum()))));
                track.setLocales(asArrayList(en_US));
                track.setUrl(buildUrl(metadata.getFilePath()));
                track.setArt(Optional.ofNullable(folder.getArt()).orElse(defaultArt));
                track.setId(Optional.of(entityIdsByTypeAndNaturalKey.get(EntityType.TRACK)
                        .get(Collections.singletonList(track.getUrl()))).get());
            });
        }
        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
        trackCatalog.setEntities(tracks);
        trackCatalog.setLocales(asArrayList(en_US));
        writeToDisk(trackCatalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-tracks.json"));
        dao.save(trackCatalog);
    }

    private Mp3Folder collectTrackInfoRecursively(File dir, int level) throws IOException, NoSuchAlgorithmException {
        List<Mp3Folder> children = new ArrayList<>();
        List<File> subdirs = Optional.ofNullable(dir.listFiles(File::isDirectory))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        for (File subdir : subdirs) {
            children.add(collectTrackInfoRecursively(subdir, level + 1));
        }
        return new Mp3Folder(collectTrackMetadata(dir), collectAlbumArt(dir), level, children);
    }

    private List<TrackMetadata> collectTrackMetadata(File dir) throws IOException {
        List<TrackMetadata> retval = new ArrayList<>();
        File destFile = new File(destDir,
                srcDir.toPath().relativize(dir.toPath()).toString().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + ".json");
        if (reset || !destFile.exists()) {
            File[] mp3s = dir.listFiles(file -> Optional.ofNullable(file)
                    .filter(File::isFile)
                    .map(File::getName)
                    .map(name -> name.length() >= MP3EXT.length() &&
                            name.substring(name.length() - MP3EXT.length()).equalsIgnoreCase(MP3EXT))
                    .orElse(false));
            if (mp3s != null && mp3s.length >= 1) {
                Arrays.stream(mp3s).map(mp3 -> {
                    Path relativePath = srcDir.toPath().relativize(mp3.toPath());
                    try {
                        TrackMetadata track = mp3Adapter.parseMetadata(mp3);
                        track.setFilePath(relativePath);
                        track.setAuthor(Optional.ofNullable(track.getAuthor()).orElse("Unknown"));
                        track.setAlbum(Optional.ofNullable(track.getAlbum()).orElse("Unknown"));
                        return track;
                    } catch (IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException e) {
                        throw new RuntimeException(String.format("Failure parsing %s.", mp3.getAbsolutePath()), e);
                    }
                }).forEach(retval::add);
            }
        }
        return retval;
    }

    private void writeToDisk(AbstractCatalog catalog, File destFile) throws IOException {
        if (writeToDisk) {
            System.out.printf("Writing to %s.%n", destFile.getAbsolutePath());
            try (FileWriter fw = new FileWriter(destFile)) {
                jsonAdapter.writeValue(fw, catalog);
            }
        }
    }

    private List<ImageMetadata> collectAlbumArt(File dir) throws NoSuchAlgorithmException {
        Optional<File[]> optionalJpgs = Optional.ofNullable(dir.listFiles(file -> Optional.ofNullable(file)
                .filter(File::isFile)
                .map(File::getName)
                .map(name -> name.length() >= JPGEXT.length() &&
                        name.substring(name.length() - JPGEXT.length()).equalsIgnoreCase(JPGEXT) &&
                        (name.startsWith("ALBUM~") || name.startsWith("AlbumArt")))
                .orElse(false)));
        MessageDigest sha256Digester = MessageDigest.getInstance("SHA-256");
        Base64.Encoder base64Encoder = Base64.getEncoder();
        return optionalJpgs.filter(jpgs -> jpgs.length >= 1).map(jpgs -> Arrays.stream(jpgs).map(jpg -> {
            Long width, height;
            String sha256Hash;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                try (FileInputStream fis = new FileInputStream(jpg);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
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
            } catch (IOException e) {
                throw new RuntimeException("Failed to read image dimensions.  File: " + jpg.getAbsolutePath() + ".", e);
            }
            String url = buildUrl(srcDir.toPath().relativize(jpg.toPath()));
            return new ImageMetadata(url, sha256Hash, width, height);
        }).distinct().collect(Collectors.toList())).orElse(null);
    }

    private String buildUrl(Path relativePath) {
        StringBuilder relativePathString = new StringBuilder();
        for (int i = 0; i < relativePath.getNameCount(); i++) {
            relativePathString.append((i == 0 ? "" : "/") +
                    UriUtils.encodePathSegment(relativePath.getName(i).toString(), StandardCharsets.UTF_8));
        }
        return String.format("%s/%s",
                baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.lastIndexOf("/")) : baseUrl,
                relativePathString.toString());
    }

    private Album newAlbumEntity(String albumName) {
        Album retval = new Album();
        retval.setLanguageOfContent(asArrayList("en"));
        retval.setNames(asArrayList(new EntityName("en", albumName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setReleaseType("Studio Album");
        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setId(UUID.randomUUID().toString());
        retval.setLocales(asArrayList(en_US));
        return retval;
    }

    private Artist newArtistEntity(String artistName) {
        Artist retval = new Artist();
        retval.setNames(asArrayList(new EntityName("en", artistName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setId(Optional.of(entityIdsByTypeAndNaturalKey.get(EntityType.ARTIST)
                .get(Collections.singletonList(artistName))).get());
//        retval.setId(UUID.randomUUID().toString());
        retval.setLocales(asArrayList(en_US));
        return retval;
    }

    private Art defaultArtObject(String baseUrl) throws IOException {
        Art retval = new Art();
        final String musicIconRelativePath = "minduka/music-icon";
        retval.setContentDescription("Music icon (A. Minduka, https://openclipart.org/detail/27648/music-icon).");
        List<ArtSource> sources = new ArrayList<>();
        for (String png : new String[]{"small", "medium", "large"}) {
            String relativePath = String.format("%s/%s.png", musicIconRelativePath, png);
            try (InputStream inputStream = getClass().getResourceAsStream(String.format("/images/%s", relativePath))) {
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                long width = bufferedImage.getWidth();
                long height = bufferedImage.getHeight();
                String url = String.format("%s/%s", baseUrl, relativePath);
                sources.add(new ArtSource(url, ArtSourceSize.valueOf(width, height), width, height));
            }
        }
        retval.setSources(sources);
        return retval;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static final class SongDTO {
        private TrackMetadata track;
        private Art albumArt;
    }
}
