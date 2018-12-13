package f18a14c09s.integration.alexa.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.MusicAlbumCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicGroupCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicRecordingCatalog;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.mp3.Mp3Adapter;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

public class PrivateMusicCataloguerCLI {
    private File srcDir;
    private File destDir;
    private Mp3Adapter mp3Adapter = new Mp3Adapter();
    private Mp3ToAlexaCatalog mp3ToAlexaCatalog = new Mp3ToAlexaCatalog();
    private ObjectMapper jsonMapper = new ObjectMapper();

    public static void main(String... args) throws IOException {
        File src = new File(args[0]);
        File dest = new File(args[1]);
        PrivateMusicCataloguerCLI cli = new PrivateMusicCataloguerCLI(src, dest);
        cli.catalogRecursively();
    }

    private PrivateMusicCataloguerCLI(File srcDir, File destDir) {
        this.srcDir = srcDir;
        this.destDir = destDir;
    }

    private void catalogRecursively() throws IOException {
        catalogRecursively(srcDir);
        catalogAlbums();
        catalogArtists();
    }

    private void catalogAlbums() throws IOException {
        ArrayList<Album> albums = new ArrayList<>(mp3ToAlexaCatalog.getAlbums());
        MusicAlbumCatalog catalog = new MusicAlbumCatalog();
        catalog.setEntities(albums);
        catalog.setLocales(newArrayList(Locale.en_US()));
        try (FileWriter fw = new FileWriter(new File(destDir,
                srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-albums.json"))) {
            jsonMapper.writeValue(fw, catalog);
        }
    }

    private void catalogArtists() throws IOException {
        ArrayList<Artist> artists = new ArrayList<>(mp3ToAlexaCatalog.getArtists());
        MusicGroupCatalog catalog = new MusicGroupCatalog();
        catalog.setEntities(artists);
        catalog.setLocales(newArrayList(Locale.en_US()));
        try (FileWriter fw = new FileWriter(new File(destDir,
                srcDir.getAbsolutePath().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + "-artists.json"))) {
            jsonMapper.writeValue(fw, catalog);
        }
    }

    private static <E> ArrayList<E> newArrayList(E... elements) {
        return Optional.ofNullable(elements)
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .collect(ArrayList::new, List::add, List::addAll);
    }

    public static final String MP3EXT = ".mp3";
    public static final String JPGEXT = ".jpg";

    private void catalogRecursively(File dir) throws IOException {
        catalogMp3s(dir);
        saveArt(dir);
        File[] subdirs = dir.listFiles(File::isDirectory);
        for (File subdir : subdirs) {
            catalogRecursively(subdir);
        }
    }

    private void catalogMp3s(File dir) throws IOException {
        File destFile = new File(destDir,
                srcDir.toPath().relativize(dir.toPath()).toString().replaceAll("[^A-Za-z0-9-_\\.]+", ".") + ".json");
        if (!destFile.exists()) {
            File[] mp3s = dir.listFiles(file -> Optional.ofNullable(file)
                    .filter(File::isFile)
                    .map(File::getName)
                    .map(name -> name.length() >= MP3EXT.length() &&
                            name.substring(name.length() - MP3EXT.length()).equalsIgnoreCase(MP3EXT))
                    .orElse(false));
            if (mp3s != null && mp3s.length >= 1) {
                ArrayList<Track> tracks = Arrays.stream(mp3s).map(mp3 -> {
                    try (FileInputStream fis = new FileInputStream(mp3)) {
                        return mp3Adapter.parseMetadata(fis);
                    } catch (UnsupportedAudioFileException | IOException e) {
                        throw new RuntimeException(String.format("Failure parsing %s.", mp3.getAbsolutePath()), e);
                    }
                }).map(mp3ToAlexaCatalog::mp3ToTrackEntity).collect(ArrayList::new, List::add, List::addAll);
                MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
                trackCatalog.setEntities(tracks);
                trackCatalog.setLocales(newArrayList(Locale.en_US()));
                System.out.printf("Writing to %s.%n", destFile.getAbsolutePath());
                try (FileWriter fw = new FileWriter(destFile)) {
                    jsonMapper.writeValue(fw, trackCatalog);
                }
            }
        }
    }

    private void saveArt(File dir) {
        File[] jpgs = dir.listFiles(file -> Optional.ofNullable(file)
                .filter(File::isFile)
                .map(File::getName)
                .map(name -> name.length() >= JPGEXT.length() &&
                        name.substring(name.length() - JPGEXT.length()).equalsIgnoreCase(JPGEXT))
                .orElse(false));
        // TODO: Perform work:
    }
}
