package f18a14c09s.integration.alexa.catalog;

import f18a14c09s.integration.alexa.data.Language;
import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.EntityFactory;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.mp3.data.TrackMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EntityFactoryTest {
    private final String expectedArtistName = "Abc";
    private final String expectedAlbumName = "Xyz 123";
    private final String expectedTrackUrl = "path://to.music/track";
    private Locale expectedLocale;
    private Art expectedArt;
    private EntityFactory subject;
    private Artist artist;
    private Album album;
    private ArtistReference artistReference;

    @BeforeEach
    void beforeEach() {
        this.expectedArt = new Art();
        this.expectedLocale = new Locale();
        this.subject = new EntityFactory(expectedLocale, null);
        this.artist = subject.newArtistEntity(expectedArtistName, expectedArt);
        this.artistReference = artist.toReference();
        this.album = subject.newAlbumEntity(expectedAlbumName, artistReference, expectedArt, 123L);
    }

    @Test
    void testNewArtistEntity() {
        Artist actualArtist = artist;
        assertNotNull(actualArtist.getArt());
        assertNotNull(actualArtist.getId());
        assertNotNull(actualArtist.getLastUpdatedTime());
        assertNotNull(actualArtist.getLocales());
        assertNotNull(actualArtist.getNames());
        assertNotNull(actualArtist.getPopularity());
        assertSame(expectedArt, actualArtist.getArt());
        assertEquals(1, actualArtist.getLocales().size());
        assertSame(expectedLocale, actualArtist.getLocales().iterator().next());
        assertEquals(1, actualArtist.getNames().size());
        assertEquals(expectedArtistName, actualArtist.getNames().iterator().next().getValue());
    }

    @Test
    void testNewAlbumEntity() {
        ArtistReference expectedArtist = artistReference;
        Album actualAlbum = album;
        assertNotNull(actualAlbum.getArtists());
        assertNotNull(actualAlbum.getArt());
        assertNotNull(actualAlbum.getReleaseType());
        assertNotNull(actualAlbum.getLanguageOfContent());
        assertNotNull(actualAlbum.getNaturalOrder());
        assertNotNull(actualAlbum.getId());
        assertNotNull(actualAlbum.getLastUpdatedTime());
        assertNotNull(actualAlbum.getLocales());
        assertNotNull(actualAlbum.getNames());
        assertNotNull(actualAlbum.getPopularity());
        assertSame(expectedArt, actualAlbum.getArt());
        assertEquals(1, actualAlbum.getLocales().size());
        assertSame(expectedLocale, actualAlbum.getLocales().iterator().next());
        assertEquals(1, actualAlbum.getNames().size());
        assertEquals(expectedAlbumName, actualAlbum.getNames().iterator().next().getValue());
        assertEquals(1, actualAlbum.getArtists().size());
        assertSame(expectedArtist, actualAlbum.getArtists().iterator().next());
        assertEquals(1, actualAlbum.getLanguageOfContent().size());
        assertEquals(Language.en, actualAlbum.getLanguageOfContent().iterator().next());
        assertTrue(Arrays.stream(ReleaseType.values())
                .map(ReleaseType::getTitle)
                .anyMatch(actualAlbum.getReleaseType()::equals));
    }

    @Test
    void testNewTrackEntity() {
        final String expectedTrackTitle = "";
        final long expectedDurationSeconds = 456, expectedTrackNumber = 123;
        ArtistReference expectedArtist = artistReference;
        AlbumReference expectedAlbum = album.toReference();
        Track actualTrack = subject.newTrackEntity(new TrackMetadata(expectedTrackTitle,
                expectedArtistName,
                expectedAlbumName,
                expectedDurationSeconds,
                null,
                null,
                null,
                expectedTrackNumber,
                null), expectedTrackUrl, expectedArtist, expectedAlbum, expectedArt);
        assertNotNull(actualTrack.getAlbums());
        assertNotNull(actualTrack.getArtists());
        assertNotNull(actualTrack.getArt());
        assertNotNull(actualTrack.getLanguageOfContent());
        assertNotNull(actualTrack.getDurationSeconds());
        assertNotNull(actualTrack.getUrl());
        assertNotNull(actualTrack.getNaturalOrder());
        assertNotNull(actualTrack.getId());
        assertNotNull(actualTrack.getLastUpdatedTime());
        assertNotNull(actualTrack.getLocales());
        assertNotNull(actualTrack.getNames());
        assertNotNull(actualTrack.getPopularity());
        assertEquals((Long) expectedTrackNumber, actualTrack.getNaturalOrder());
        assertEquals((Long) expectedDurationSeconds, actualTrack.getDurationSeconds());
        assertEquals(expectedTrackUrl, actualTrack.getUrl());
        assertEquals(1, actualTrack.getArtists().size());
        assertSame(expectedArtist, actualTrack.getArtists().iterator().next());
        assertEquals(1, actualTrack.getAlbums().size());
        assertSame(expectedAlbum, actualTrack.getAlbums().iterator().next());
        assertSame(expectedArt, actualTrack.getArt());
        assertEquals(1, actualTrack.getLocales().size());
        assertSame(expectedLocale, actualTrack.getLocales().iterator().next());
        assertEquals(1, actualTrack.getNames().size());
        assertEquals(expectedTrackTitle, actualTrack.getNames().iterator().next().getValue());
        assertEquals(1, actualTrack.getLanguageOfContent().size());
        assertEquals(Language.en, actualTrack.getLanguageOfContent().iterator().next());
    }
}
