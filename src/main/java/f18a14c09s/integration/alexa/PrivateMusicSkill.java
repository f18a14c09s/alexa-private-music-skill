package f18a14c09s.integration.alexa;

import com.amazon.ask.AlexaSkill;
import com.amazon.ask.request.SkillRequest;
import com.amazon.ask.response.SkillResponse;
import com.amazon.ask.response.impl.BaseSkillResponse;
import com.amazon.ask.util.impl.JacksonJsonMarshaller;
import com.fasterxml.jackson.core.JsonProcessingException;
import f18a14c09s.integration.alexa.data.BaseMessage;
import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.*;
import f18a14c09s.integration.json.JSONAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class PrivateMusicSkill implements AlexaSkill<Request, Response> {
    private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private JSONAdapter jsonAdapter = new JSONAdapter();
    private static final Map<RequestType, AbstractMusicSkill<? extends Request<?>, ? extends Response<?>>>
            skillsByRequestType;

    static {
        Map<RequestType, AbstractMusicSkill<? extends Request<?>, ? extends Response<?>>> tempMap = new HashMap<>();
        tempMap.put(new RequestType(AlexaMediaSearch.NAMESPACE_NAME, AlexaMediaSearch.GET_PLAYABLE_CONTENT.getMyName()),
                new GetPlayableContentSkill());
        tempMap.put(new RequestType(AlexaMediaPlayback.NAMESPACE_NAME, AlexaMediaPlayback.INITIATE.getMyName()),
                new InitiationSkill());
        tempMap.put(new RequestType(AlexaMediaPlayQueue.NAMESPACE_NAME, AlexaMediaPlayQueue.GET_ITEM.getMyName()),
                new GetItemSkill());
        tempMap.put(new RequestType(AlexaMediaPlayQueue.NAMESPACE_NAME,
                AlexaAudioPlayQueue.GET_PREVIOUS_ITEM.getMyName()), new GetPreviousItemSkill());
        tempMap.put(new RequestType(AlexaMediaPlayQueue.NAMESPACE_NAME, AlexaAudioPlayQueue.GET_NEXT_ITEM.getMyName()),
                new GetNextItemSkill());
        skillsByRequestType = tempMap;
    }

    @Override
    public SkillResponse<Response> execute(SkillRequest skillRequest, Object context) {
        Optional<Request> request;
        try {
            request = Optional.ofNullable(jsonAdapter.readValue(skillRequest.getRawRequest(), Request.class));
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
        request.ifPresent(this::debug);
        return request.map(BaseMessage::getHeader)
                .map(messageHeader -> new RequestType(messageHeader.getNamespace(), messageHeader.getName()))
                .map(skillsByRequestType::get)
                .map(skill -> skill.invoke(request.get(), context))
                .map(response -> new BaseSkillResponse(new JacksonJsonMarshaller(), response))
                .orElse(null);
    }

    private void debug(Request request) {
        String messageId = request.getHeader().getMessageId();
        try {
            logger.debug(String.format("Processing request %s:%n%s",
                    messageId,
                    jsonAdapter.writeValueAsString(request)));
        } catch (JsonProcessingException e) {
            logger.warn(String.format("Failed to print request %s.", messageId), e);
        }
    }
}
