package f18a14c09s.integration.alexa.music.catalog;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import static f18a14c09s.util.CollectionUtil.asArrayList;

@Getter
@Setter(AccessLevel.PRIVATE)
public class AlbumKey implements Comparable<AlbumKey> {
    private String artistName;
    private String albumName;

    public AlbumKey(String artistName, String albumName) {
        Objects.requireNonNull(artistName);
        Objects.requireNonNull(albumName);
        this.artistName = artistName;
        this.albumName = albumName;
    }

    @Override
    public String toString() {
        return String.format("Album %s by %s", albumName, artistName);
    }

    @Override
    public boolean equals(Object o) {
        AlbumKey rhs = o instanceof AlbumKey ? (AlbumKey) o : null;
        return rhs != null && Objects.equals(artistName, rhs.artistName) && Objects.equals(albumName, rhs.albumName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artistName, albumName);
    }

    public List<String> toList() {
        return asArrayList(getArtistName(), getAlbumName());
    }

    @Override
    public int compareTo(AlbumKey rhs) {
        return Comparator.comparing(AlbumKey::getArtistName).thenComparing(AlbumKey::getAlbumName).compare(this, rhs);
    }
}
