package f18a14c09s.integration.mp3.data;

import f18a14c09s.integration.alexa.music.catalog.AlbumKey;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSource;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;
import java.util.stream.*;

import static f18a14c09s.integration.mp3.data.ImageMetadata.getExactlyOnePerSize;

public class Mp3Folder {
    @Getter
    private List<TrackMetadata> mp3s;

    @Getter(AccessLevel.PRIVATE)
    private List<ImageMetadata> images;

    @Getter
    private List<Mp3Folder> children;

    @Getter
    private int hierarchyLevel;

    @Getter
    private Art art;

    @Getter
    private String relativePath;

    public Mp3Folder(List<TrackMetadata> mp3s,
                     List<ImageMetadata> images,
                     int hierarchyLevel,
                     List<Mp3Folder> children,
                     String relativePath) {
        this.mp3s = mp3s;
        this.images = images;
        this.hierarchyLevel = hierarchyLevel;
        this.children = children;
        this.relativePath = relativePath;
        this.art = toArt(images);
    }

    private static Art toArt(List<ImageMetadata> images) {
        return Optional.ofNullable(getExactlyOnePerSize(images))
                .map(Map::entrySet)
                .map(Collection::stream)
                .map(stream -> stream.map(entry -> new ArtSource(entry.getValue().getUrl(),
                        entry.getKey(),
                        entry.getValue().getWidth(),
                        entry.getValue().getHeight())).collect(Collectors.toList()))
                .map(Art::new)
                .orElse(null);
    }

    public boolean isLikelyAlbumFolder() {
        return (getMp3s() != null && !getMp3s().isEmpty()) || (getHierarchyLevel() >= 2 && getChildren() != null &&
                getChildren().stream().anyMatch(Mp3Folder::isLikelyAlbumFolder));
    }

    public boolean isLikelyArtistFolder() {
        return !isLikelyAlbumFolder() && getHierarchyLevel() == 1 && getChildren() != null &&
                getChildren().stream().anyMatch(Mp3Folder::isLikelyAlbumFolder);
    }

    public Set<AlbumKey> getUniqueAlbumNames() {
        Set<AlbumKey> retval = new HashSet<>();
        Optional.ofNullable(getMp3s())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .filter(Objects::nonNull)
                .map(track -> new AlbumKey(track.getAuthor(), track.getAlbum()))
                .forEach(retval::add);
        return retval;
    }

    public Set<String> getUniqueArtistNames() {
        Set<String> retval = new HashSet<>();
        Optional.ofNullable(getMp3s())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(TrackMetadata::getAuthor)
                .filter(Objects::nonNull)
                .forEach(retval::add);
        return retval;
    }

    public Set<Long> getUniqueReleaseYears() {
        Set<Long> retval = new HashSet<>();
        Optional.ofNullable(getMp3s())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(TrackMetadata::getYear)
                .filter(Objects::nonNull)
                .forEach(retval::add);
        return retval;
    }
}
