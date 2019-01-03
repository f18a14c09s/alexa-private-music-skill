package f18a14c09s.integration.mp3;

import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSourceSize;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@Getter
@Setter
public class Mp3Folder {
    private List<TrackMetadata> mp3s;

    private List<ImageMetadata> images;

    private List<Mp3Folder> children;

    private int hierarchyLevel;

    public Mp3Folder() {
    }

    public Mp3Folder(List<TrackMetadata> mp3s, List<ImageMetadata> images, int hierarchyLevel) {
        this.mp3s = mp3s;
        this.images = images;
        this.hierarchyLevel = hierarchyLevel;
    }

    public Collection<ImageMetadata> getExactlyOneImagePerArtSourceSize() {
        if (getImages() == null || getImages().isEmpty()) {
            return getImages();
        } else {
            Map<ArtSourceSize, ImageMetadata> imagesBySize = getImages().stream()
                    .collect(Collectors.groupingBy(ImageMetadata::getArtSourceSize))
                    .values()
                    .stream()
                    .map(Iterable::iterator)
                    .map(Iterator::next)
                    .collect(Collectors.toMap(ImageMetadata::getArtSourceSize, UnaryOperator.identity()));
            for (ArtSourceSize size : Arrays.stream(ArtSourceSize.values())
                    .filter(size -> !imagesBySize.containsKey(size))
                    .collect(Collectors.toSet())) {
                for (ArtSourceSize altSize : size.getAlternateSizesByPriority()) {
                    if (imagesBySize.containsKey(altSize)) {
                        imagesBySize.put(size, imagesBySize.get(altSize));
                        break;
                    }
                }
            }
            return imagesBySize.values();
        }
    }

    public Art toArt() {
        return new Art(null,
                getExactlyOneImagePerArtSourceSize().stream()
                        .map(ImageMetadata::toArtSource)
                        .collect(Collectors.toList()));
    }

    public boolean isLikelyAlbumFolder() {
        return (getMp3s() != null && !getMp3s().isEmpty()) || (getHierarchyLevel() >= 2 && getChildren() != null &&
                getChildren().stream().anyMatch(Mp3Folder::isLikelyAlbumFolder));
    }

    public boolean isLikelyArtistFolder() {
        return !isLikelyAlbumFolder() && getHierarchyLevel() == 1 && getChildren() != null &&
                getChildren().stream().anyMatch(Mp3Folder::isLikelyAlbumFolder);
    }

    public Set<String> getUniqueAlbumNames() {
        Set<String> retval = new HashSet<>();
        Optional.ofNullable(getMp3s())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(TrackMetadata::getAlbum)
                .forEach(retval::add);
        Optional.ofNullable(getChildren())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(Mp3Folder::getUniqueAlbumNames)
                .forEach(retval::addAll);
        return retval;
    }

    public Set<String> getUniqueArtistNames() {
        Set<String> retval = new HashSet<>();
        Optional.ofNullable(getMp3s())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(TrackMetadata::getAuthor)
                .forEach(retval::add);
        Optional.ofNullable(getChildren())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(Mp3Folder::getUniqueArtistNames)
                .forEach(retval::addAll);
        return retval;
    }
}
