package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.mp3.TrackMetadata;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static f18a14c09s.integration.alexa.data.Language.en;
import static f18a14c09s.integration.alexa.music.entities.ReleaseType.StudioAlbum;

public class EntityFactory {
    private Locale defaultLocale;

    public EntityFactory(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Artist newArtistEntity(String artistName, Art art) {
        Artist retval = new Artist();
        retval.setNames(List.of(new EntityName(en, artistName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setLastUpdatedTime(ZonedDateTime.now());
        retval.setId(UUID.randomUUID().toString());
        retval.setLocales(List.of(defaultLocale));
        retval.setArt(art);
        return retval;
    }

    public Album newAlbumEntity(String albumName, ArtistReference artistReference, Art art) {
        Album retval = new Album();
        retval.setLanguageOfContent(List.of(en));
        retval.setNames(List.of(new EntityName(en, albumName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setReleaseType(StudioAlbum.getTitle());
        retval.setLastUpdatedTime(ZonedDateTime.now());
        retval.setId(UUID.randomUUID().toString());
        retval.setLocales(List.of(defaultLocale));
        retval.setArtists(List.of(artistReference));
        retval.setArt(art);
        return retval;
    }

    public Track newTrackEntity(TrackMetadata mp3, String url, Collection<ArtistReference> artists,
                                AlbumReference album, Art art) {
        Track retval = new Track();
        retval.setLanguageOfContent(List.of(en));
        retval.setNames(List.of(new EntityName(en, mp3.getTitle())));
        retval.setDurationSeconds(mp3.getDurationSeconds());
        retval.setNaturalOrder(mp3.getTrackNumber());
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setLastUpdatedTime(ZonedDateTime.now());
        retval.setArtists(List.copyOf(artists));
        retval.setAlbums(List.of(album));
        retval.setLocales(List.of(defaultLocale));
        retval.setUrl(url);
        retval.setArt(art);
        retval.setId(trackIdFromUrl(url));
        return retval;
    }

    public static String trackIdFromUrl(String url) {
        return UUID.nameUUIDFromBytes(
                String.format(
                        "MUSIC%sS3KEY%s",
                        EntityType.TRACK.name(),
                        url
                ).getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}
