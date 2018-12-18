package f18a14c09s.integration.alexa.catalog;

import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.entities.EntityName;
import f18a14c09s.integration.alexa.music.entities.Popularity;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.mp3.TrackMetadata;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Mp3ToAlexaCatalog {
//    private Map<String, Artist> artists = new HashMap<>();
//    private Map<String, Map<String, Album>> albums = new HashMap<>();

    private String toUuid(String str) {
        return UUID.nameUUIDFromBytes(str.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Popularity defaultPopularity() {
        return new Popularity(0L, null);
    }

    private ArrayList<Locale> defaultLocales() {
        return asArrayList(new Locale("US", "en"));
    }

    private ArrayList<String> defaultContentLanguages() {
        return asArrayList("en");
    }

    private <E> ArrayList<E> asArrayList(E... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    public Track mp3ToTrackEntity(TrackMetadata mp3) {
        Track retval = new Track();
//        Album album = albums.computeIfAbsent(mp3.getAuthor(), key -> new HashMap<>())
//                .computeIfAbsent(mp3.getAlbum(), key -> mp3ToAlbumEntity(mp3));
//        retval.setAlbums(asArrayList(album.toReference()));
//        retval.setAlternateNames();
//        retval.setArtists(album.getArtists());
        retval.setLanguageOfContent(defaultContentLanguages());
        retval.setNames(asArrayList(new EntityName("en", mp3.getTitle())));
        retval.setPopularity(defaultPopularity());
        retval.setReleaseType("Studio Recording");
        retval.setDeleted(false);
        retval.setLastUpdatedTime(Calendar.getInstance());
//        retval.setId(toUuid(Optional.ofNullable(mp3.getTitle()).orElse("Unknown")));
        retval.setId(UUID.randomUUID().toString());
        retval.setLocales(defaultLocales());
        return retval;
    }

//    public Collection<Album> getAlbums() {
//        return albums.entrySet()
//                .stream()
//                .flatMap(artist -> artist.getValue().values().stream())
//                .collect(Collectors.toList());
//    }
//
//    public Collection<Artist> getArtists() {
//        return artists.values();
//    }
}
