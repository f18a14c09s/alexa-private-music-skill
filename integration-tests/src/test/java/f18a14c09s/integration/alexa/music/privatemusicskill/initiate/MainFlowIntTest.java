package f18a14c09s.integration.alexa.music.privatemusicskill.initiate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.lambda.LambdaClient;

import static org.junit.jupiter.api.Assertions.fail;

public class MainFlowIntTest {
    static LambdaClient lambdaClient;
    static ObjectMapper jsonMapper;

    @BeforeAll
    static void beforeAll() {
        lambdaClient = LambdaClient.builder().build();
        jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "artist", "album", "track"
            }
    )
    void testByEntityType() {
        fail("Test not implemented.");
    }
}
