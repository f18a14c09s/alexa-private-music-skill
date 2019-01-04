package f18a14c09s.integration.mp3;

import f18a14c09s.integration.alexa.music.data.ArtSource;
import f18a14c09s.integration.alexa.music.data.ArtSourceSize;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class ImageMetadata {
    private String url;
    private String sha256Hash;
    private Long width;
    private Long height;

    public ImageMetadata() {
    }

    public ImageMetadata(String url, String sha256Hash, Long width, Long height) {
        this.url = url;
        this.sha256Hash = sha256Hash;
        this.width = width;
        this.height = height;
    }

    public ArtSourceSize getArtSourceSize() {
        return ArtSourceSize.valueOf(getWidth(), getHeight());
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
}
