package f18a14c09s.integration.alexa.music.catalog;

import org.junit.jupiter.api.Test;

import javax.persistence.Persistence;
import java.util.*;

class PrivateMusicCataloguerCLITest {
    @Test
    void testMain() {
        Map<String, Object> cfg = new HashMap<>();
        Persistence.generateSchema("alexa-music-data", cfg);
    }
}
