package f18a14c09s.integration.alexa.music.data;

import f18a14c09s.integration.alexa.data.SpeechInfo;
import f18a14c09s.integration.json.JSONAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GetPlayableContentResponseTest {
    @Test
    void testUnmarshalling() {
        GetPlayableContentResponse actual = Assertions.assertDoesNotThrow(() -> new JSONAdapter().readValueFromResource(
                "/example-data/GetPlayableContent.Response-example.json",
                GetPlayableContentResponse.class));
        assertNotNull(actual);
        assertNotNull(actual.getHeader());
        assertNotNull(actual.getPayload());
        assertNotNull(actual.getPayload().getContent());
        assertNotNull(actual.getPayload().getContent().getMetadata());
        assertNotNull(actual.getPayload().getContent().getMetadata().getName());
        assertNotNull(actual.getPayload().getContent().getMetadata().getName().getSpeech());
        assertEquals("2cae4d53-6bc1-4f8f-aa98-7dd2727ca84b", actual.getHeader().getMessageId());
        assertEquals("Alexa.Media.Search", actual.getHeader().getNamespace());
        assertEquals("GetPlayableContent.Response", actual.getHeader().getName());
        assertEquals("1.0", actual.getHeader().getPayloadVersion());
        assertEquals("1021012f-12bb-4938-9723-067a4338b6d0", actual.getPayload().getContent().getId());
        assertEquals(MediaMetadata.Type.ARTIST, actual.getPayload().getContent().getMetadata().getType());
        assertEquals(SpeechInfo.SpeechType.PLAIN_TEXT,
                actual.getPayload().getContent().getMetadata().getName().getSpeech().getType());
        assertEquals("sade", actual.getPayload().getContent().getMetadata().getName().getSpeech().getText());
        assertEquals("Sade", actual.getPayload().getContent().getMetadata().getName().getDisplay());
    }
}