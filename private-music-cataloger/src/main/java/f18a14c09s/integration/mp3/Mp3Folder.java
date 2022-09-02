package f18a14c09s.integration.mp3;

import f18a14c09s.integration.alexa.music.catalog.AlbumKey;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSource;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static f18a14c09s.integration.mp3.ImageMetadata.getExactlyOnePerSize;

public class Mp3Folder {
    @Getter
    private List<TrackMetadata> mp3s;

    @Getter(AccessLevel.PRIVATE)
    private List<ImageMetadata> images;

    @Getter
    private List<Mp3Folder> children;

    @Getter(AccessLevel.PRIVATE)
    private int hierarchyLevel;

    @Getter
    private Art art;

    public Mp3Folder(List<TrackMetadata> mp3s,
                     List<ImageMetadata> images,
                     int hierarchyLevel,
                     List<Mp3Folder> children) {
        this.mp3s = mp3s;
        this.images = images;
        this.hierarchyLevel = hierarchyLevel;
        this.children = children;
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
        Optional.ofNullable(getMp3s()).stream().flatMap(Collection::stream)
                .flatMap(trackMetadata -> Optional.of(trackMetadata.getDistinctArtistNames())
                        .map(Map::keySet)
                        .filter(Predicate.not(Set::isEmpty))
                        .orElse(Set.of("Unknown"))
                        .stream()
                        .map(
                                artistName -> new AlbumKey(artistName, Optional.ofNullable(trackMetadata.getAlbum())
                                        .orElse("Unknown"))
                        ))
                .forEach(retval::add);
        return retval;
    }

    public Set<String> getUniqueArtistNames() {
        Set<String> retval = new HashSet<>();
        Optional.ofNullable(getMp3s()).stream().flatMap(Collection::stream)
                .flatMap(trackMetadata -> Optional.of(trackMetadata.getDistinctArtistNames())
                        .map(Map::keySet)
                        .filter(Predicate.not(Set::isEmpty))
                        .orElse(Set.of("Unknown")).stream())
                .forEach(retval::add);
        return retval;
    }

    public Stream<TrackMetadata> recurseMp3s() {
        return Stream.concat(
                getMp3s().stream(),
                getChildren().stream().flatMap(Mp3Folder::recurseMp3s)
        );
    }
}
