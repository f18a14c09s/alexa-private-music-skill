package f18a14c09s.integration.alexa.music.privatemusicskill.getplayablecontent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MainFlowIntTest {
    static LambdaClient lambdaClient;
    static ObjectMapper jsonMapper;

    @BeforeAll
    static void beforeAll() {
        lambdaClient = LambdaClient.builder().build();
        jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    SdkBytes requestPayload(String testCaseFilenamePrefix) {
        return assertDoesNotThrow(() -> {
            try (InputStream testCaseInputStream = getClass().getClassLoader().getResourceAsStream(
                    String.format(
                            "privatemusicskill/getplayablecontent/%s_request.json",
                            testCaseFilenamePrefix
                    )
            )) {
                return SdkBytes.fromInputStream(testCaseInputStream);
            }
        });
    }

    Map<?, ?> expectedDecodedPayload(String testCaseFilenamePrefix) {
        return assertDoesNotThrow(() -> {
            try (InputStream expectedResponseInputStream = getClass().getClassLoader().getResourceAsStream(
                    String.format(
                            "privatemusicskill/getplayablecontent/%s_response.json",
                            testCaseFilenamePrefix
                    )
            )) {
                return jsonMapper.readValue(expectedResponseInputStream, Map.class);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "artist", "album", "track"
            }
    )
    void testByEntityType(
            String testCaseFilenamePrefix
    ) {
        InvokeResponse actualResponse = lambdaClient.invoke(InvokeRequest.builder().functionName(
                "privateMusicAlexaSkill"
        ).payload(
                requestPayload(testCaseFilenamePrefix)
        ).build());

        assertNotNull(actualResponse);
        String actualPayloadJson = actualResponse.payload().asUtf8String();
        assertNotNull(actualPayloadJson);
        Map<?,?> actualResponsePayload = assertDoesNotThrow(
                () ->jsonMapper.readValue(actualResponse.payload().asUtf8String(), Map.class)
        );
        assertEquals(
                actualResponsePayload,
                expectedDecodedPayload(testCaseFilenamePrefix),
                actualResponse.payload().asUtf8String()
        );
    }
}
