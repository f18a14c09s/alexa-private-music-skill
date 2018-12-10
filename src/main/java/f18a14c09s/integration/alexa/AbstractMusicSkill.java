package f18a14c09s.integration.alexa;

import com.fasterxml.jackson.core.JsonProcessingException;
import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.Request;
import f18a14c09s.integration.alexa.music.messagetypes.Response;
import f18a14c09s.integration.json.JSONAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public abstract class AbstractMusicSkill<RequestImpl extends Request<?>, ResponseImpl extends Response<?>>
//        implements         AlexaSkill<Request, ResponseImpl>
{
    private List<RequestType> supportedRequestTypes = new ArrayList<>();
    private Class<RequestImpl> requestClass;
    private final Logger logger = LogManager.getLogger(getClass());
    private final JSONAdapter jsonAdapter = new JSONAdapter();

    public AbstractMusicSkill(Class<RequestImpl> requestClass,
                              RequestType requestType0,
                              RequestType... additionalRequestTypes) {
//        super(JacksonJsonUnmarshaller.withTypeBinding(Request.class, "header"), new JacksonJsonMarshaller<>());
        this.setRequestClass(requestClass);
        Objects.requireNonNull(requestType0, "First argument required.");
        this.getSupportedRequestTypes().add(requestType0);
        Optional.ofNullable(additionalRequestTypes)
                .map(Arrays::asList)
                .ifPresent(this.getSupportedRequestTypes()::addAll);
    }

    private void debug(Request request) {
        String messageId = request.getHeader().getMessageId();
        try {
            getLogger().debug(String.format("Processing request %s:%n%s",
                    messageId,
                    getJsonAdapter().writeValueAsString(request)));
        } catch (JsonProcessingException e) {
            getLogger().warn(String.format("Failed to print request %s.", messageId), e);
        }
    }

    //    @Override
    protected ResponseImpl invoke(Request request, Object context) {
        debug(request);
        return invokeImpl((RequestImpl) request, context);
    }

    protected abstract ResponseImpl invokeImpl(RequestImpl request, Object context);

//    @Override
//    public SkillResponse<ResponseImpl> execute(SkillRequest request, Object context) {
//        Optional<RequestImpl> deserializedRequest = unmarshaller.unmarshall(request.getRawRequest());
//        Optional<MessageHeader> messageHeader = deserializedRequest.map(RequestImpl::getHeader);
//        MusicRequestType requestType = messageHeader.map(MessageHeader::toMusicRequestType).orElse(null);
//        if (requestType == null || supportedMusicRequestTypes == null ||
//                !supportedMusicRequestTypes.contains(requestType)) {
//            getLogger().debug(String.format(
//                    "This skill will ignore request %s which has unrelated type %s.  Supported types: %s.",
//                    messageHeader.map(MessageHeader::getMessageId).orElse("(with unknown ID)"),
//                    requestType,
//                    supportedMusicRequestTypes));
//            return null;
//        }
//        return super.execute(request, context);
//    }
}
