package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.catalog.AlbumKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static f18a14c09s.util.CollectionUtil.asArrayList;
import static org.junit.jupiter.api.Assertions.*;

class AlbumKeyTest {
    private final String expectedArtistName = "Abc";
    private final String expectedAlbumName = "Xyz 123";
    private AlbumKey subject;

    @BeforeEach
    void beforeEach() {
        this.subject = new AlbumKey(expectedArtistName, expectedAlbumName);
    }

    @Test
    void testConstructorWithValidArguments() {
        assertEquals(expectedArtistName, subject.getArtistName());
        assertEquals(expectedAlbumName, subject.getAlbumName());
    }

    @Test
    void testConstructorWithNullArgument() {
        assertThrows(NullPointerException.class, () -> new AlbumKey(null, null));
        assertThrows(NullPointerException.class, () -> new AlbumKey(expectedArtistName, null));
        assertThrows(NullPointerException.class, () -> new AlbumKey(null, expectedAlbumName));
    }

    @Test
    void testToString() {
        assertNotNull(subject.toString());
        assertTrue(subject.toString().contains(expectedArtistName));
        assertTrue(subject.toString().contains(expectedAlbumName));
    }

    @Test
    void testEquals() {
        assertTrue(subject.equals(subject));
        assertTrue(subject.equals(new AlbumKey(expectedArtistName, expectedAlbumName)));
        assertFalse(subject.equals(new AlbumKey("Other " + expectedArtistName, expectedAlbumName)));
        assertFalse(subject.equals(new AlbumKey(expectedArtistName, "Other " + expectedAlbumName)));
    }

    @Test
    void testHashCode() {
        assertEquals(75778762, subject.hashCode());
    }

    @Test
    void testToList() {
        assertNotNull(subject.toList());
        assertEquals(asArrayList(expectedArtistName, expectedAlbumName), subject.toList());
    }

    @Test
    void testCompareTo() {
        assertEquals(0, subject.compareTo(subject));
        assertEquals(0, subject.compareTo(new AlbumKey(expectedArtistName, expectedAlbumName)));
        assertTrue(new AlbumKey("abc", "def").compareTo(new AlbumKey("ghi", "def")) < 0);
        assertTrue(new AlbumKey("abc", "def").compareTo(new AlbumKey("abc", "jkl")) < 0);
        assertTrue(new AlbumKey("ghi", "def").compareTo(new AlbumKey("abc", "def")) > 0);
        assertTrue(new AlbumKey("abc", "jkl").compareTo(new AlbumKey("abc", "def")) > 0);
    }
}
