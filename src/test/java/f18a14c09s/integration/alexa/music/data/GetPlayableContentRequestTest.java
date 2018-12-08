package f18a14c09s.integration.alexa.music.data;

import f18a14c09s.integration.json.JSONAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

class GetPlayableContentRequestTest {
    @Test
    void testUnmarshalling() {
        GetPlayableContentRequest actual = Assertions.assertDoesNotThrow(() -> new JSONAdapter().readValueFromResource(
                "/example-data/GetPlayableContent.Request-example.json",
                GetPlayableContentRequest.class));
        Assertions.assertNotNull(actual);
        Assertions.assertNotNull(actual.getHeader());
        Assertions.assertNotNull(actual.getPayload());
        Assertions.assertNotNull(actual.getPayload().getFilters());
        Assertions.assertNotNull(actual.getPayload().getSelectionCriteria());
        Assertions.assertNotNull(actual.getPayload().getRequestContext());
        Assertions.assertNotNull(actual.getPayload().getRequestContext().getLocation());
        Assertions.assertNotNull(actual.getPayload().getRequestContext().getUser());
        Assertions.assertNotNull(actual.getPayload().getSelectionCriteria().getAttributes());
        Assertions.assertEquals("2cae4d53-6bc1-4f8f-aa98-7dd2727ca84b", actual.getHeader().getMessageId());
        Assertions.assertEquals("Alexa.Media.Search", actual.getHeader().getNamespace());
        Assertions.assertEquals("GetPlayableContent", actual.getHeader().getName());
        Assertions.assertEquals("1.0", actual.getHeader().getPayloadVersion());
        Assertions.assertEquals("amzn1.ask.account.AGF3NETIE4MNXNG2Z64Z27RXB6JCK2R62BCPYUZI",
                actual.getPayload().getRequestContext().getUser().getId());
        Assertions.assertEquals("e72e16c7e42f292c6912e7710c838347ae178b4a",
                actual.getPayload().getRequestContext().getUser().getAccessToken());
        Assertions.assertEquals("US", actual.getPayload().getRequestContext().getLocation().getCountryCode());
        Assertions.assertEquals("en-US", actual.getPayload().getRequestContext().getLocation().getOriginatingLocale());
        Assertions.assertEquals(true, actual.getPayload().getFilters().getExplicitLanguageAllowed());
        Assertions.assertEquals(2, actual.getPayload().getSelectionCriteria().getAttributes().size());
        Assertions.assertEquals(EntityType.TRACK,
                actual.getPayload().getSelectionCriteria().getAttributes().get(0).getType());
        Assertions.assertEquals(EntityType.MEDIA_TYPE,
                actual.getPayload().getSelectionCriteria().getAttributes().get(1).getType());
        assertInstanceOf(actual.getPayload().getSelectionCriteria().getAttributes().get(0),
                ResolvedSelectionCriteria.BasicEntityAttribute.class);
        assertInstanceOf(actual.getPayload().getSelectionCriteria().getAttributes().get(1),
                ResolvedSelectionCriteria.MediaTypeAttribute.class);
        Assertions.assertEquals("138545995",
                Optional.of(actual.getPayload().getSelectionCriteria().getAttributes().get(0))
                        .map(ResolvedSelectionCriteria.BasicEntityAttribute.class::cast)
                        .get()
                        .getEntityId());
        Assertions.assertEquals(ResolvedSelectionCriteria.MediaTypeAttrValue.TRACK,
                Optional.of(actual.getPayload().getSelectionCriteria().getAttributes().get(1))
                        .map(ResolvedSelectionCriteria.MediaTypeAttribute.class::cast)
                        .get()
                        .getValue());
    }

    private void assertInstanceOf(Object o, Class<?> clazz) {
        Assertions.assertTrue(o != null && clazz.isAssignableFrom(o.getClass()),
                String.format("Object %s (class %s) is not an instance of %s.",
                        o,
                        Optional.ofNullable(o).map(Object::getClass).map(Class::getName).orElse(null),
                        clazz));
    }
}