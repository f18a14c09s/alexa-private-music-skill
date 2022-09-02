package f18a14c09s.integration.mp3;

import f18a14c09s.integration.alexa.music.data.ArtSourceSize;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@Getter
@Setter
public class ImageMetadata {
    private String url;
    private String sha256Hash;
    private Long width;
    private Long height;
    private ArtSourceSize artSourceSize;

    public ImageMetadata() {
    }

    public ImageMetadata(String url, String sha256Hash, Long width, Long height) {
        this(url, sha256Hash, width, height, ArtSourceSize.valueOf(width, height));
    }

    public ImageMetadata(String url, String sha256Hash, Long width, Long height, ArtSourceSize artSourceSize) {
        this.url = url;
        this.sha256Hash = sha256Hash;
        this.width = width;
        this.height = height;
        this.artSourceSize = artSourceSize;
    }

    @Override
    public int hashCode() {
        return Optional.ofNullable(sha256Hash).map(String::hashCode).orElse(0);
    }

    @Override
    public boolean equals(Object rhsO) {
        ImageMetadata rhs = rhsO instanceof ImageMetadata ? (ImageMetadata) rhsO : null;
        return sha256Hash != null && rhs != null && rhs.sha256Hash != null && sha256Hash.equals(rhs.sha256Hash);
    }

    public static Map<ArtSourceSize, ImageMetadata> getExactlyOnePerSize(List<ImageMetadata> images) {
        if (images == null || images.isEmpty()) {
            return null;
        } else {
            Map<ArtSourceSize, ImageMetadata> imagesBySize = images.stream()
                    .filter(image -> image.getArtSourceSize() != null)
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
            return imagesBySize;
        }
    }
}
