package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.mp3.TrackMetadata;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static f18a14c09s.integration.alexa.data.Language.en;
import static f18a14c09s.integration.alexa.music.entities.ReleaseType.StudioAlbum;
import static f18a14c09s.util.CollectionUtil.asArrayList;

public class EntityFactory {
    private Locale defaultLocale;
    private Map<EntityType, Map<List<String>, String>> entityIdsByTypeAndNaturalKey;

    public EntityFactory(Locale defaultLocale, Map<EntityType, Map<List<String>, String>> existingEntities) {
        this.defaultLocale = defaultLocale;
        this.entityIdsByTypeAndNaturalKey = Optional.ofNullable(existingEntities).orElse(Arrays.stream(EntityType.values()).collect(
                Collectors.toMap(
                        Function.identity(),
                        entityType -> new HashMap<>()
                )
        ));
    }

    public Artist newArtistEntity(String artistName, Art art) {
        Artist retval = new Artist();
        retval.setNames(asArrayList(new EntityName(en, artistName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
//        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setLastUpdatedTime(ZonedDateTime.now());
        retval.setId(
                Optional.ofNullable(
                        entityIdsByTypeAndNaturalKey.get(
                                EntityType.ARTIST
                        ).get(Collections.singletonList(artistName))
                ).orElseGet(() -> UUID.randomUUID().toString())
        );
        retval.setLocales(asArrayList(defaultLocale));
        retval.setArt(art);
        return retval;
    }

    public Album newAlbumEntity(String albumName, ArtistReference artistReference, Art art) {
        String artistName = Optional.ofNullable(artistReference)
                .map(BaseEntityReference::getNames)
                .map(List::iterator)
                .filter(Iterator::hasNext)
                .map(Iterator::next)
                .map(EntityName::getValue)
                .orElse(null);
        Album retval = new Album();
        retval.setLanguageOfContent(asArrayList(en));
        retval.setNames(asArrayList(new EntityName(en, albumName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setReleaseType(StudioAlbum.getTitle());
//        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setLastUpdatedTime(ZonedDateTime.now());
        retval.setId(Optional.ofNullable(entityIdsByTypeAndNaturalKey.get(EntityType.ALBUM)
                .get(asArrayList(artistName, albumName))).orElseGet(() -> UUID.randomUUID().toString()));
        retval.setLocales(asArrayList(defaultLocale));
        retval.setArtists(asArrayList(artistReference));
        retval.setArt(art);
        return retval;
    }

    public Track newTrackEntity(TrackMetadata mp3, String url, ArtistReference artist, AlbumReference album, Art art) {
        Track retval = new Track();
        retval.setLanguageOfContent(asArrayList(en));
        retval.setNames(asArrayList(new EntityName(en, mp3.getTitle())));
        retval.setDurationSeconds(mp3.getDurationSeconds());
        retval.setNaturalOrder(mp3.getTrackNumber());
        retval.setPopularity(Popularity.unratedWithNoOverrides());
//        retval.setReleaseType(StudioAlbum.getTitle());
//        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setLastUpdatedTime(ZonedDateTime.now());
        retval.setArtists(asArrayList(artist));
        retval.setAlbums(asArrayList(album));
        retval.setLocales(asArrayList(defaultLocale));
        retval.setUrl(url);
        retval.setArt(art);
//        String artistName = artist.getNames().get(0).getValue();
//        String albumName = album.getNames().get(0).getValue();
        retval.setId(
                UUID.nameUUIDFromBytes(
                        String.format(
                                "MUSIC%sS3KEY%s",
                                EntityType.TRACK.name(),
                                url
                        ).getBytes(StandardCharsets.UTF_8)
                ).toString()
        );
        return retval;
    }
}
