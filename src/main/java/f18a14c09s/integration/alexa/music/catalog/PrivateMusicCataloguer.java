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
    private boolean reset;
    private boolean writeToDisk;
    private boolean writeToDb;
    private File srcDir;
    private File destDir;
    private String baseUrl;
    private CatalogDAO dao;
    private EntityFactory entityFactory;
    private Mp3Adapter mp3Adapter;
    private JSONAdapter jsonAdapter;
    private MessageDigest sha256Digester;
    private Base64.Encoder base64Encoder;
    private Locale en_US;
    private Art defaultArt;

    PrivateMusicCataloguer(File srcDir,
                           File destDir,
                           String baseUrl,
                           boolean reset,
                           String imageBaseUrl,
                           boolean writeToDb) throws IOException, NoSuchAlgorithmException {
        this.srcDir = srcDir;
        this.destDir = destDir;
        this.baseUrl = baseUrl;
        this.reset = reset;
        this.writeToDisk = (destDir != null);
        this.writeToDb = writeToDb;
        this.mp3Adapter = new Mp3Adapter();
        this.jsonAdapter = new JSONAdapter();
        this.sha256Digester = MessageDigest.getInstance("SHA-256");
        this.base64Encoder = Base64.getEncoder();
        //
        this.defaultArt = defaultArtObject(imageBaseUrl);
        this.en_US = Locale.en_US();
        Map<EntityType, Map<List<String>, String>> entityIdsByTypeAndNaturalKey = null;
        if (writeToDb) {
            this.dao = new CatalogDAO(Hbm2DdlAuto.create);
            dao.save(en_US);
            dao.save(defaultArt);
            dao.commit();
            en_US = dao.findLocale(en_US.getCountry(), en_US.getLanguage());
            entityIdsByTypeAndNaturalKey = dao.getCataloguedEntityIdsByTypeAndNaturalKey();
        }
        this.entityFactory = new EntityFactory(en_US, entityIdsByTypeAndNaturalKey);
    }

    void catalogMusic() throws IOException, NoSuchAlgorithmException {
//        System.out.printf("Locale %s-%s has ID %s.%n", en_US.getLanguage(), en_US.getCountry(), en_US.getId());
        Mp3Folder rootMp3Folder = collectTrackInfoRecursively(srcDir, 0);
        printFolderSummary(rootMp3Folder);
        if (writeToDb) {
            Map<String, Artist> artists = catalogArtists(rootMp3Folder);
            Map<String, String> artistIdToName = artists.entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
            Map<String, ArtistReference> artistReferences = writeToDb ?
                    dao.findAllArtistReferences()
                            .stream()
                            .collect(Collectors.toMap(artist -> artistIdToName.get(artist.getId()),
                                    UnaryOperator.identity())) :
                    artists.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toReference()));
            Map<ArrayList<String>, Album> albums = catalogAlbums(rootMp3Folder, artistReferences);
            Map<String, ArrayList<String>> albumIdToName = albums.entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
            Map<ArrayList<String>, AlbumReference> albumReferences = writeToDb ?
                    dao.findAllAlbumReferences()
                            .stream()
                            .collect(Collectors.toMap(album -> albumIdToName.get(album.getId()),
                                    UnaryOperator.identity())) :
                    albums.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toReference()));
            catalogTracks(rootMp3Folder, artistReferences, albumReferences);
            if (writeToDb) {
                printDbSummary();
                dao.close(true);
            }
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
                    artists.put(artistName, entityFactory.
                            newArtistEntity(artistName, Optional.ofNullable(folder.getArt()).orElse(defaultArt)));
                }
            });
        }
        MusicGroupCatalog catalog = new MusicGroupCatalog();
        catalog.setEntities(new ArrayList<>(artists.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-artists.json"));
        if (writeToDb) {
            dao.save(catalog);
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
                    albums.put(keyAsList, entityFactory.
                            newAlbumEntity(albumKey.getAlbumName(),
                                    artists.get(albumKey.getArtistName()),
                                    Optional.ofNullable(folder.getArt()).orElse(defaultArt)));
                }
            });
        }
        MusicAlbumCatalog catalog = new MusicAlbumCatalog();
        catalog.setEntities(new ArrayList<>(albums.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-albums.json"));
        if (writeToDb) {
            dao.save(catalog);
        }
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
            folder.getMp3s()
                    .forEach(metadata -> tracks.add(entityFactory.newTrackEntity(metadata,
                            buildUrl(metadata.getFilePath()),
                            artists.get(metadata.getAuthor()),
                            albums.get(asArrayList(metadata.getAuthor(), metadata.getAlbum())),
                            Optional.ofNullable(folder.getArt()).orElse(defaultArt))));
        }
        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
        trackCatalog.setEntities(tracks);
        trackCatalog.setLocales(asArrayList(en_US));
        writeToDisk(trackCatalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-tracks.json"));
        if (writeToDb) {
            dao.save(trackCatalog);
        }
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

    private List<TrackMetadata> collectTrackMetadata(File dir) {
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
                        track.setTitle(Optional.ofNullable(track.getTitle()).orElse("Unknown"));
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

    private List<ImageMetadata> collectAlbumArt(File dir) {
        Optional<File[]> optionalJpgs = Optional.ofNullable(dir.listFiles(file -> Optional.ofNullable(file)
                .filter(File::isFile)
                .map(File::getName)
                .map(name -> name.length() >= JPGEXT.length() &&
                        name.substring(name.length() - JPGEXT.length()).equalsIgnoreCase(JPGEXT) &&
                        (name.startsWith("ALBUM~") || name.startsWith("AlbumArt")))
                .orElse(false)));
        return optionalJpgs.filter(jpgs -> jpgs.length >= 1).map(jpgs -> Arrays.stream(jpgs).map(jpg -> {
            try (FileInputStream fis = new FileInputStream(jpg)) {
                return newImageMetadata(fis, buildUrl(srcDir.toPath().relativize(jpg.toPath())));
            } catch (IOException e) {
                throw new RuntimeException("Failed to access image " + jpg.getAbsolutePath() + ".", e);
            }
        }).distinct().collect(Collectors.toList())).orElse(null);
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
