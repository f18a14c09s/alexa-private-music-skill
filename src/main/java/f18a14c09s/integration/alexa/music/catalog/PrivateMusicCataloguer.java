package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.data.Locale;
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
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static f18a14c09s.util.CollectionUtil.asArrayList;

class PrivateMusicCataloguer {
    public static final String MP3EXT = ".mp3";
    public static final String JPGEXT = ".jpg";
    public static final String ROOT_S3_PREFIX = "MP3_Albums/";
    //    private boolean reset;
//    private boolean writeToDisk;
//    private boolean writeToDb;
//    private File srcDir;
    //    private File destDir;
    private String srcS3Bucket;
    private String baseUrl;
    private CatalogDAO dao;
    private EntityFactory entityFactory;
    private Mp3Adapter mp3Adapter;
    private JSONAdapter jsonAdapter;
    private MessageDigest sha256Digester;
    private Base64.Encoder base64Encoder;
    private S3Client s3Client = S3Client.create();
    private Locale en_US;
    private Art defaultArt;

    PrivateMusicCataloguer(File srcDir,
                           File destDir,
                           String baseUrl,
                           boolean reset,
                           String imageBaseUrl,
                           boolean writeToDb,
                           File srcCatalogDir) throws IOException, NoSuchAlgorithmException {
        this.srcS3Bucket = "music.francisjohnson.org";
//        this.destDir = destDir;
        this.baseUrl = baseUrl;
//        this.reset = reset;
//        this.writeToDisk = (destDir != null);
//        this.writeToDb = writeToDb;
        this.mp3Adapter = new Mp3Adapter();
        this.jsonAdapter = new JSONAdapter();
        this.sha256Digester = MessageDigest.getInstance("SHA-256");
        this.base64Encoder = Base64.getEncoder();
        //
        this.defaultArt = defaultArtObject(imageBaseUrl);
        this.en_US = Locale.en_US();
        Map<EntityType, Map<List<String>, String>> entityIdsByTypeAndNaturalKey = null;
        this.dao = new CatalogDAO(Hbm2DdlAuto.create);
        dao.save(en_US);
        dao.save(defaultArt);
        dao.commit();
        en_US = dao.findLocale(en_US.getCountry(), en_US.getLanguage());
        entityIdsByTypeAndNaturalKey = Map.of(
                EntityType.ARTIST,
                new HashMap<>(),
                EntityType.ALBUM,
                new HashMap<>(),
                EntityType.TRACK,
                new HashMap<>());
        // entityIdsByTypeAndNaturalKey =
        // dao.getCataloguedEntityIdsByTypeAndNaturalKey();
//        Map<EntityType, Map<List<String>, Set<String>>> entityIdsByTypeAndNaturalKeyWithPotentialDuplicates = CatalogDirectoryReader
//                .readCatalogDirectory(
//                        Map.of(
//                                EntityType.ARTIST,
//                                new File(srcCatalogDir, "AMAZON.MusicGroup Catalog 8.json"),
//                                EntityType.ALBUM,
//                                new File(srcCatalogDir, "AMAZON.MusicAlbum Catalog 1643.json"),
//                                EntityType.TRACK,
//                                new File(srcCatalogDir, "AMAZON.MusicRecording Catalog 3680.json")));
//        for (EntityType entityType : entityIdsByTypeAndNaturalKeyWithPotentialDuplicates.keySet()) {
//            for (List<String> entityKey : entityIdsByTypeAndNaturalKeyWithPotentialDuplicates.get(entityType)
//                    .keySet()) {
//                Set<String> entityIds = entityIdsByTypeAndNaturalKeyWithPotentialDuplicates.get(entityType)
//                        .get(entityKey);
//                if (entityIds.size() > 1) {
//                    System.out.printf(
//                            "%s,%s has multiple entity IDs:%s%n",
//                            entityType,
//                            entityKey,
//                            entityIds.stream().map(entityId -> String.format("%n\t%s", entityId))
//                                    .collect(Collectors.joining()));
//                }
//                entityIdsByTypeAndNaturalKey.get(entityType).put(
//                        entityKey,
//                        entityIds.iterator().next()
//                );
//            }
//        }
        this.entityFactory = new EntityFactory(en_US, entityIdsByTypeAndNaturalKey);
    }

    void catalogMusic() throws IOException, NoSuchAlgorithmException {
        // System.out.printf("Locale %s-%s has ID %s.%n", en_US.getLanguage(),
        // en_US.getCountry(), en_US.getId());
        Mp3Folder rootMp3Folder = collectTrackInfoRecursivelyS3(ROOT_S3_PREFIX, 0);
        printFolderSummary(rootMp3Folder);
        Map<String, Artist> artists = catalogArtists(rootMp3Folder);
        Map<String, String> artistIdToName = artists.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
        Map<String, ArtistReference> artistReferences = dao.findAllArtistReferences()
                .stream()
                .collect(Collectors.toMap(artist -> artistIdToName.get(artist.getId()),
                        UnaryOperator.identity()));
        Map<ArrayList<String>, Album> albums = catalogAlbums(rootMp3Folder, artistReferences);
        Map<String, ArrayList<String>> albumIdToName = albums.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
        Map<ArrayList<String>, AlbumReference> albumReferences = dao.findAllAlbumReferences()
                .stream()
                .collect(Collectors.toMap(album -> albumIdToName.get(album.getId()),
                        UnaryOperator.identity()));
        catalogTracks(rootMp3Folder, artistReferences, albumReferences);
        printDbSummary();
        dao.close(true);
        dao.commit();
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
                        .filter(child -> child.isLikelyArtistFolder() && child.getUniqueArtistNames().size() != 1)
                        .map(child -> String.format("%n\t\tNon-Unique Names (%s total): %s",
                                child.getUniqueArtistNames().size(),
                                child.getUniqueArtistNames()))
                        .collect(Collectors.joining()),
                rootMp3Folder.getChildren()
                        .stream()
                        .flatMap(child -> Stream.concat(Stream.of(child),
                                child.getChildren() == null ? Stream.empty() : child.getChildren().stream()))
                        .filter(child -> child.isLikelyAlbumFolder() && child.getUniqueAlbumNames().size() != 1)
                        .map(child -> String.format("%n\t\tNon-Unique Names (%s total): %s",
                                child.getUniqueAlbumNames().size(),
                                child.getUniqueAlbumNames()))
                        .collect(Collectors.joining()));
    }

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

    private Map<String, Artist> catalogArtists(Mp3Folder rootMp3Folder) throws IOException {
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
                    albums.put(keyAsList, entityFactory.newAlbumEntity(albumKey.getAlbumName(),
                            artists.get(albumKey.getArtistName()),
                            Optional.ofNullable(folder.getArt()).orElse(defaultArt)));
                }
            });
        }
        MusicAlbumCatalog catalog = new MusicAlbumCatalog();
        catalog.setEntities(new ArrayList<>(albums.values()));
        catalog.setLocales(asArrayList(en_US));
        dao.save(catalog);
        return albums;
    }

    private void catalogTracks(Mp3Folder rootMp3Folder,
                               Map<String, ArtistReference> artists,
                               Map<ArrayList<String>, AlbumReference> albums) throws IOException {
        List<Track> tracks = new ArrayList<>();
        List<Mp3Folder> mp3Folders = asArrayList(rootMp3Folder);
        Map<String, String> trackUrlsById = new HashMap<>();
        while (!mp3Folders.isEmpty()) {
            Mp3Folder folder = mp3Folders.remove(0);
            mp3Folders.addAll(folder.getChildren());
            for (TrackMetadata metadata : folder.getMp3s()) {
                Track trackEntity = entityFactory.newTrackEntity(metadata,
                        buildUrl(metadata.getFilePath()),
                        artists.get(metadata.getAuthor()),
                        albums.get(asArrayList(metadata.getAuthor(), metadata.getAlbum())),
                        Optional.ofNullable(folder.getArt()).orElse(defaultArt));
                if (trackUrlsById.containsKey(trackEntity.getId())) {
                    System.out.printf(
                            "Track appears to be a duplicate:%n\tEntity ID: %s%n\tTrack URL: %s%n\tPotential Duplicate URL: %s%n",
                            trackEntity.getId(),
                            trackUrlsById.get(trackEntity.getId()),
                            trackEntity.getUrl()
                    );
                } else {
                    tracks.add(trackEntity);
                    trackUrlsById.put(trackEntity.getId(), trackEntity.getUrl());
                }
            }
        }
        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
        trackCatalog.setEntities(tracks);
        trackCatalog.setLocales(asArrayList(en_US));
        dao.save(trackCatalog);
    }

    // private Mp3Folder collectTrackInfoRecursively(File dir, int level) throws IOException, NoSuchAlgorithmException {
    //     List<Mp3Folder> children = new ArrayList<>();
    //     List<File> subdirs = Optional.ofNullable(dir.listFiles(File::isDirectory))
    //             .map(Arrays::asList)
    //             .orElse(Collections.emptyList());
    //     for (File subdir : subdirs) {
    //         children.add(collectTrackInfoRecursively(subdir, level + 1));
    //     }
    //     return new Mp3Folder(collectTrackMetadata(dir), collectAlbumArt(dir), level, children);
    // }

    private Mp3Folder collectTrackInfoRecursivelyS3(String s3Prefix, int level) {
        List<Mp3Folder> children = new ArrayList<>();
        List<CommonPrefix> commonPrefixes = new ArrayList<>();
        List<S3Object> s3Objects = new ArrayList<>();
        for (ListObjectsV2Response response : s3Client.listObjectsV2Paginator(
                ListObjectsV2Request.builder().bucket(
                        srcS3Bucket
                ).delimiter(
                        "/" // DO NOT forget this!
                ).prefix(
                        s3Prefix
                ).build()
        )) {
            commonPrefixes.addAll(response.commonPrefixes());
            s3Objects.addAll(response.contents());
        }
        List<TrackMetadata> mp3s = collectTrackMetadataS3(s3Objects);
        // TODO: Remove this:
        System.out.printf(
                "Found %s MP3s at S3 prefix %s.%n",
                mp3s.size(),
                s3Prefix
        );
        if (!mp3s.isEmpty()) {
            commonPrefixes.clear();
        }
        for (CommonPrefix subdir : commonPrefixes) {
            Mp3Folder child = collectTrackInfoRecursivelyS3(subdir.prefix(), level + 1);
            children.add(child);
            System.out.printf(
                    "Found %s MP3s under child S3 prefix %s.%n",
                    child.recurseMp3s().count(),
                    subdir
            );
            // TODO: Remove this:
            if (child.recurseMp3s().findAny().isPresent()) {
                break;
            }
        }
        return new Mp3Folder(mp3s, collectAlbumArtS3(s3Objects), level, children);
    }

    // private List<TrackMetadata> collectTrackMetadata(File dir) {
    //     List<TrackMetadata> retval = new ArrayList<>();
    //     File destFile = new File(destDir,
    //             srcDir.toPath().relativize(dir.toPath()).toString().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + ".json");
    //     if (reset || !destFile.exists()) {
    //         File[] mp3s = dir.listFiles(file -> Optional.ofNullable(file)
    //                 .filter(File::isFile)
    //                 .map(File::getName)
    //                 .map(name -> name.length() >= MP3EXT.length() &&
    //                         name.substring(name.length() - MP3EXT.length()).equalsIgnoreCase(MP3EXT))
    //                 .orElse(false));
    //         if (mp3s != null && mp3s.length >= 1) {
    //             Arrays.stream(mp3s).map(mp3 -> {
    //                 Path relativePath = srcDir.toPath().relativize(mp3.toPath());
    //                 try {
    //                     TrackMetadata track = mp3Adapter.parseMetadata(mp3);
    //                     track.setFilePath(relativePath);
    //                     track.setAuthor(Optional.ofNullable(track.getAuthor()).orElse("Unknown"));
    //                     track.setAlbum(Optional.ofNullable(track.getAlbum()).orElse("Unknown"));
    //                     track.setTitle(Optional.ofNullable(track.getTitle()).orElse("Unknown"));
    //                     return track;
    //                 } catch (IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException e) {
    //                     throw new RuntimeException(String.format("Failure parsing %s.", mp3.getAbsolutePath()), e);
    //                 }
    //             }).forEach(retval::add);
    //         }
    //     }
    //     return retval;
    // }

    private static String removeRootS3Prefix(String s3PrefixOrKey) {
        return s3PrefixOrKey.replaceAll("^" + Pattern.quote(ROOT_S3_PREFIX), "");
    }

    private TrackMetadata parseTrackMetadataS3(S3Object s3Object) {
        System.out.printf(
                "Retrieving S3 object %s.%n",
                s3Object.key()
        );
        try (InputStream s3InputStream = s3Client.getObject(
                GetObjectRequest.builder().bucket(
                        srcS3Bucket
                ).key(
                        s3Object.key()
                ).build()
        )) {
            TrackMetadata track = mp3Adapter.parseMetadata(
                    s3InputStream
            );
            track.setFilePath(removeRootS3Prefix(s3Object.key()));
            track.setAuthor(Optional.ofNullable(track.getAuthor()).orElse("Unknown"));
            track.setAlbum(Optional.ofNullable(track.getAlbum()).orElse("Unknown"));
            track.setTitle(Optional.ofNullable(track.getTitle()).orElse("Unknown"));
            return track;
        } catch (IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException e) {
            throw new RuntimeException(String.format("Failure retrieving or parsing %s.", s3Object.key()), e);
        }
    }

    private List<TrackMetadata> collectTrackMetadataS3(List<S3Object> s3Objects) {
        List<TrackMetadata> retval = new ArrayList<>();
        List<S3Object> mp3s = s3Objects.stream().filter(s3Object -> s3Object.key().toLowerCase().endsWith(MP3EXT)).collect(Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<TrackMetadata>> tasks;
        try {
            tasks = executor.invokeAll(mp3s.stream().<Callable<TrackMetadata>>map(s3Object -> (() -> parseTrackMetadataS3(s3Object))).collect(Collectors.toList()));
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        for (Future<TrackMetadata> task : tasks) {
            try {
                retval.add(task.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
//        if (mp3s.size() >= 1) {
//            mp3s.stream().map(mp3 -> {
//                try (InputStream s3InputStream = s3Client.getObject(
//                        GetObjectRequest.builder().bucket(
//                                srcS3Bucket
//                        ).key(
//                                mp3.key()
//                        ).build()
//                )) {
//                    TrackMetadata track = mp3Adapter.parseMetadata(
//                            s3InputStream
//                    );
//                    track.setFilePath(removeRootS3Prefix(mp3.key()));
//                    track.setAuthor(Optional.ofNullable(track.getAuthor()).orElse("Unknown"));
//                    track.setAlbum(Optional.ofNullable(track.getAlbum()).orElse("Unknown"));
//                    track.setTitle(Optional.ofNullable(track.getTitle()).orElse("Unknown"));
//                    return track;
//                } catch (IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException e) {
//                    throw new RuntimeException(String.format("Failure parsing %s.", mp3.key()), e);
//                }
//            }).forEach(retval::add);
//        }
        return retval;
    }

//    private void writeToDisk(AbstractCatalog catalog, File destFile) throws IOException {
//        if (writeToDisk) {
//            System.out.printf("Writing to %s.%n", destFile.getAbsolutePath());
//            try (FileWriter fw = new FileWriter(destFile)) {
//                jsonAdapter.writeValue(fw, catalog);
//            }
//        }
//    }

    // private List<ImageMetadata> collectAlbumArt(File dir) {
    //     Optional<File[]> optionalJpgs = Optional.ofNullable(dir.listFiles(file -> Optional.ofNullable(file)
    //             .filter(File::isFile)
    //             .map(File::getName)
    //             .map(name -> name.length() >= JPGEXT.length() &&
    //                     name.substring(name.length() - JPGEXT.length()).equalsIgnoreCase(JPGEXT) &&
    //                     (name.startsWith("ALBUM~") || name.startsWith("AlbumArt")))
    //             .orElse(false)));
    //     return optionalJpgs.filter(jpgs -> jpgs.length >= 1).map(jpgs -> Arrays.stream(jpgs).map(jpg -> {
    //         try (FileInputStream fis = new FileInputStream(jpg)) {
    //             return newImageMetadata(fis, buildUrl(srcDir.toPath().relativize(jpg.toPath())));
    //         } catch (IOException e) {
    //             throw new RuntimeException("Failed to access image " + jpg.getAbsolutePath() + ".", e);
    //         }
    //     }).distinct().collect(Collectors.toList())).orElse(null);
    // }

    private List<ImageMetadata> collectAlbumArtS3(List<S3Object> s3Objects) {
        List<S3Object> jpgs = s3Objects.stream()
                .filter(
                        s3Object -> Arrays.stream(s3Object.key().toLowerCase().split("/")).reduce((x, y) -> y).map(name ->
                                (name.startsWith("ALBUM~") || name.startsWith("AlbumArt")) && name.endsWith(JPGEXT)).orElse(false)
                ).collect(Collectors.toList());
        return jpgs.stream().map(jpg -> {
            try (InputStream s3InputStream = s3Client.getObject(GetObjectRequest.builder().bucket(srcS3Bucket).key(jpg.key()).build())) {
                return newImageMetadata(s3InputStream, buildUrl(removeRootS3Prefix(jpg.key())));
            } catch (IOException e) {
                throw new RuntimeException("Failed to access image " + jpg.key() + ".", e);
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

//    private String buildUrl(Path relativePath) {
//        StringBuilder relativePathString = new StringBuilder();
//        for (int i = 0; i < relativePath.getNameCount(); i++) {
//            relativePathString.append((i == 0 ? "" : "/") +
//                    UriUtils.encodePathSegment(relativePath.getName(i).toString(), StandardCharsets.UTF_8));
//        }
//        return String.format("%s/%s",
//                baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.lastIndexOf("/")) : baseUrl,
//                relativePathString.toString());
//    }

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
