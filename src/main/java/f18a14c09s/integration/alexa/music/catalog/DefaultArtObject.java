package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSource;
import f18a14c09s.integration.alexa.music.data.ArtSourceSize;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DefaultArtObject extends Art {
    public DefaultArtObject(String baseUrl) throws IOException {
        final String musicIconRelativePath = "minduka/music-icon";
        setContentDescription("Music icon (A. Minduka, https://openclipart.org/detail/27648/music-icon).");
        List<ArtSource> sources = new ArrayList<>();
        for (String png : new String[]{"small", "medium", "large"}) {
            String relativePath = String.format("%s/%s.png", musicIconRelativePath, png);
            try (InputStream inputStream = getClass().getResourceAsStream(String.format("/images/%s", relativePath))) {
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                long width = bufferedImage.getWidth();
                long height = bufferedImage.getHeight();
                String url = String.format("%s/%s", baseUrl, relativePath);
                sources.add(new ArtSource(url, ArtSourceSize.valueOf(width, height), width, height));
            }
        }
        setSources(sources);
    }
}
