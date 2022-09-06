package f18a14c09s.integration.alexa;

import com.fasterxml.jackson.core.JsonProcessingException;
import f18a14c09s.integration.alexa.music.catalog.CatalogService;
import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.Request;
import f18a14c09s.integration.alexa.music.messagetypes.Response;
import f18a14c09s.integration.json.JSONAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public abstract class AbstractMusicSkill<RequestImpl extends Request<?>, ResponseImpl extends Response<?>> {
    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private List<RequestType> supportedRequestTypes = new ArrayList<>();
    private Class<RequestImpl> requestClass;
    private final JSONAdapter jsonAdapter = new JSONAdapter();
    private CatalogService catalogService;

    public AbstractMusicSkill(Class<RequestImpl> requestClass,
                              RequestType requestType0,
                              RequestType... additionalRequestTypes) {
        this.setRequestClass(requestClass);
        Objects.requireNonNull(requestType0, "First argument required.");
        this.getSupportedRequestTypes().add(requestType0);
        Optional.ofNullable(additionalRequestTypes)
                .map(Arrays::asList)
                .ifPresent(this.getSupportedRequestTypes()::addAll);
        try {
            catalogService = new CatalogService();
        } catch (IOException e) {
            LOGGER.error("Unable to setup catalog DAO and/or service.", e);
            throw new RuntimeException(e);
        }
    }

    private void debug(Request<?> request) {
        String messageId = request.getHeader().getMessageId();
        try {
            LOGGER.debug(String.format("Processing request %s:%n%s",
                    messageId,
                    getJsonAdapter().writeValueAsString(request)));
        } catch (JsonProcessingException e) {
            LOGGER.warn(String.format("Failed to print request %s.", messageId), e);
        }
    }

    protected ResponseImpl invoke(Request request, Object context) {
        debug(request);
        return invokeImpl((RequestImpl) request, context);
    }

    protected abstract ResponseImpl invokeImpl(RequestImpl request, Object context);
}
