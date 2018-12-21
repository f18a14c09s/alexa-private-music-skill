package f18a14c09s.integration.alexa.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicAlbumCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicGroupCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicRecordingCatalog;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSource;
import f18a14c09s.integration.alexa.music.data.ArtSourceSize;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.mp3.Mp3Adapter;
import f18a14c09s.integration.mp3.TrackMetadata;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class PrivateMusicCataloguerCLI {
    public static final String MP3EXT = ".mp3";
    public static final String JPGEXT = ".jpg";
    private Map<String, Map<String, List<Track>>> retval = new HashMap<>();
    private boolean reset = true;
    private boolean writeToDisk = false;
    private File srcDir;
    private File destDir;
    private String baseUrl;
    private CatalogDAO dao = new CatalogDAO();
    private Mp3Adapter mp3Adapter = new Mp3Adapter();
    private Mp3ToAlexaCatalog mp3ToAlexaCatalog = new Mp3ToAlexaCatalog();
    private ObjectMapper jsonMapper = new ObjectMapper();
    private Locale en_US = Locale.en_US();

    public static void main(String... args) throws IOException {
        File src = new File(args[0]);
        File dest = new File(args[1]);
        String baseUrl = args[2];
        PrivateMusicCataloguerCLI cli = new PrivateMusicCataloguerCLI(src, dest, baseUrl);
        cli.catalogRecursively();
    }

    private PrivateMusicCataloguerCLI(File srcDir, File destDir, String baseUrl) throws IOException {
        this.srcDir = srcDir;
        this.destDir = destDir;
        this.baseUrl = baseUrl;
    }

    private void catalogRecursively() throws IOException {
        dao.save(en_US);
        dao.commit();
        en_US = dao.findLocale(en_US.getCountry(), en_US.getLanguage());
        System.out.printf("Locale %s-%s has ID %s%n.", en_US.getLanguage(), en_US.getCountry(), en_US.getId());
        List<SongDTO> trackMetadataAndAlbumArt = catalogRecursively(srcDir);
        trackMetadataAndAlbumArt.stream().map(SongDTO::getTrack).forEach(track -> {
            track.setAlbum(Optional.ofNullable(track.getAlbum()).filter(s -> !s.trim().isEmpty()).orElse("Unknown"));
            track.setAuthor(Optional.ofNullable(track.getAuthor()).filter(s -> !s.trim().isEmpty()).orElse("Unknown"));
        });
        Map<String, String> artistIdToName = catalogArtists(trackMetadataAndAlbumArt).entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
        Map<String, ArtistReference> artists = dao.findAllArtistReferences()
                .stream()
                .collect(Collectors.toMap(artist -> artistIdToName.get(artist.getId()), UnaryOperator.identity()));
        Map<String, ArrayList<String>> albumIdToName = catalogAlbums(trackMetadataAndAlbumArt, artists).entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
        Map<ArrayList<String>, AlbumReference> albums = dao.findAllAlbumReferences()
                .stream()
                .collect(Collectors.toMap(album -> albumIdToName.get(album.getId()),
                        UnaryOperator.identity(),
                        (lhs, rhs) -> {
                            if (Objects.equals(lhs.getId(), rhs.getId()) ||
                                    Objects.equals(albumIdToName.get(lhs.getId()), albumIdToName.get(rhs.getId()))) {
                                System.out.printf("Duplicate:%n\tLHS: %s%n\t\t%s%n\tRHS: %s%n\t\t%s%n",
                                        lhs,
                                        albumIdToName.get(lhs.getId()),
                                        rhs,
                                        albumIdToName.get(rhs.getId()));
                            }
                            return lhs;
                        }));
        List<Track> tracks = trackMetadataAndAlbumArt.stream().map(trackAndArt -> {
            TrackMetadata metadata = trackAndArt.getTrack();
            Track track = mp3ToAlexaCatalog.mp3ToTrackEntity(metadata);
            track.setArtists(asArrayList(artists.get(metadata.getAuthor())));
            track.setAlbums(asArrayList(albums.get(asArrayList(metadata.getAuthor(), metadata.getAlbum()))));
            track.setLocales(asArrayList(en_US));
            track.setUrl(buildUrl(metadata.getFilePath()));
            track.setArt(trackAndArt.getAlbumArt());
            return track;
        }).collect(Collectors.toList());
        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
        trackCatalog.setEntities(tracks);
        trackCatalog.setLocales(asArrayList(en_US));
        writeToDisk(trackCatalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-tracks.json"));
        dao.save(trackCatalog);
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
                PopularityOverride.class)) {
            System.out.printf("Total %s: %s%n", clazz.getSimpleName(), dao.count(clazz));
        }
        dao.close(true);
    }

    private Map<ArrayList<String>, Album> catalogAlbums(List<SongDTO> tracks,
                                                        Map<String, ArtistReference> artists) throws IOException {
        Map<ArrayList<String>, Album> albums = tracks.stream()
                .map(SongDTO::getTrack)
                .map(track -> asArrayList(track.getAuthor(), track.getAlbum()))
                .distinct()
                .collect(Collectors.toMap(UnaryOperator.identity(), artistAlbum -> {
                    Album album = newAlbumEntity(artistAlbum.get(1));
                    album.setArtists(asArrayList(artists.get(artistAlbum.get(0))));
                    return album;
                }));
        albums.values().forEach(album -> album.setArt(tracks.get(0).getAlbumArt()));
        MusicAlbumCatalog catalog = new MusicAlbumCatalog();
        catalog.setEntities(new ArrayList<>(albums.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-albums.json"));
        dao.save(catalog);
        return albums;
    }

    private Map<String, Artist> catalogArtists(List<SongDTO> tracks) throws IOException {
        Map<String, Artist> artists = tracks.stream()
                .map(SongDTO::getTrack)
                .map(TrackMetadata::getAuthor)
                .distinct()
                .collect(Collectors.toMap(UnaryOperator.identity(), this::newArtistEntity));
        MusicGroupCatalog catalog = new MusicGroupCatalog();
        artists.values().forEach(artist -> artist.setArt(tracks.get(0).getAlbumArt()));
        catalog.setEntities(new ArrayList<>(artists.values()));
        catalog.setLocales(asArrayList(en_US));
        writeToDisk(catalog,
                new File(destDir, srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-artists.json"));
        dao.save(catalog);
        return artists;
    }

    private static <E> ArrayList<E> asArrayList(E... elements) {
        return Optional.ofNullable(elements)
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .collect(ArrayList::new, List::add, List::addAll);
    }

    private List<SongDTO> catalogRecursively(File dir) throws IOException {
        List<SongDTO> retval = new ArrayList<>();
        Art albumArt = saveArt(dir);
        retval.addAll(catalogMp3s(dir).stream()
                .map(track -> new SongDTO(track, albumArt))
                .collect(Collectors.toList()));
        File[] subdirs = dir.listFiles(File::isDirectory);
        for (File subdir : subdirs) {
            retval.addAll(catalogRecursively(subdir));
        }
        return retval;
    }

    private List<TrackMetadata> catalogMp3s(File dir) throws IOException {
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
                    try (FileInputStream fis = new FileInputStream(mp3)) {
                        TrackMetadata track = mp3Adapter.parseMetadata(fis);
                        track.setFilePath(relativePath);
                        return track;
                    } catch (UnsupportedAudioFileException | IOException e) {
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
                jsonMapper.writeValue(fw, catalog);
            }
        }
    }

    private Art saveArt(File dir) {
        Optional<File[]> optionalJpgs = Optional.ofNullable(dir.listFiles(file -> Optional.ofNullable(file)
                .filter(File::isFile)
                .map(File::getName)
                .map(name -> name.length() >= JPGEXT.length() &&
                        name.substring(name.length() - JPGEXT.length()).equalsIgnoreCase(JPGEXT) &&
                        (name.startsWith("ALBUM~") || name.startsWith("AlbumArt")))
                .orElse(false)));
        return optionalJpgs.filter(jpgs -> jpgs.length >= 1).map(jpgs -> new Art(null, Arrays.stream(jpgs).map(jpg -> {
            Long width, height;
            try {
                BufferedImage image = ImageIO.read(jpg);
                if (image == null) {
                    width = null;
                    height = null;
                } else {
                    width = (long) image.getWidth();
                    height = (long) image.getHeight();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read image dimensions.  File: " + jpg.getAbsolutePath() + ".", e);
            }
            return new ArtSource(buildUrl(dir.toPath().relativize(jpg.toPath())),
                    ArtSourceSize.valueOf(width, height),
                    width,
                    height);
        }).collect(Collectors.toList()))).orElse(null);
    }

    private String buildUrl(Path relativePath) {
        return String.format("%s/%s",
                baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.lastIndexOf("/")) : baseUrl,
                relativePath.toUri().toString());
    }

    private Album newAlbumEntity(String albumName) {
        Album retval = new Album();
        retval.setLanguageOfContent(asArrayList("en"));
        retval.setNames(asArrayList(new EntityName("en", albumName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setReleaseType("Studio Album");
        retval.setDeleted(false);
        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setId(UUID.randomUUID().toString());
        retval.setLocales(asArrayList(en_US));
        return retval;
    }

    private Artist newArtistEntity(String artistName) {
        Artist retval = new Artist();
        retval.setNames(asArrayList(new EntityName("en", artistName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setDeleted(false);
        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setId(UUID.randomUUID().toString());
        retval.setLocales(asArrayList(en_US));
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
