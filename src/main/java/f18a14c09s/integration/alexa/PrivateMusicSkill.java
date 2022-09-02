package f18a14c09s.integration.alexa;

import com.amazon.ask.AlexaSkill;
import com.amazon.ask.request.SkillRequest;
import com.amazon.ask.response.SkillResponse;
import com.amazon.ask.response.impl.BaseSkillResponse;
import com.amazon.ask.util.impl.JacksonJsonMarshaller;
import com.fasterxml.jackson.core.JsonProcessingException;
import f18a14c09s.integration.alexa.data.AbstractMessage;
import f18a14c09s.integration.alexa.data.BaseMessage;
import f18a14c09s.integration.alexa.data.MessageHeader;
import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.*;
import f18a14c09s.integration.json.JSONAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PrivateMusicSkill implements AlexaSkill<Request<?>, Response<?>> {
    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
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
        tempMap.put(new RequestType(AlexaAudioPlayQueue.NAMESPACE_NAME,
                AlexaAudioPlayQueue.GET_PREVIOUS_ITEM.getMyName()), new GetPreviousItemSkill());
        tempMap.put(new RequestType(AlexaAudioPlayQueue.NAMESPACE_NAME, AlexaAudioPlayQueue.GET_NEXT_ITEM.getMyName()),
                new GetNextItemSkill());
        skillsByRequestType = tempMap;
    }

    @Override
    public SkillResponse<Response<?>> execute(SkillRequest skillRequest, Object context) {
        // In theory, the Alexa Skills Kit SDK will
        Request<?> request;
        String rawSkillRequestString;
        try {
            rawSkillRequestString = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(
                            ByteBuffer.wrap(skillRequest.getRawRequest())
                    )
                    .toString();
        } catch (IOException e) {
            LOGGER.error("Failed to parse raw skill request as a UTF-8 string.", e);
            return null;
        }
        LOGGER.debug(String.format(
                "Raw skill request string:%n%s",
                rawSkillRequestString
        ));
        try {
            request = jsonAdapter.readValue(rawSkillRequestString, Request.class);
        } catch (IOException e) {
            LOGGER.error("Failed to parse skill request string as JSON.", e);
            return null;
        }
        if (request == null) {
            LOGGER.error("Raw skill request JSON parsed as null value.");
            return null;
        }
        debug(request);
        AbstractMusicSkill<?, ?> skillOperation = Optional.of(request).map(BaseMessage::getHeader)
                .map(messageHeader -> new RequestType(messageHeader.getNamespace(), messageHeader.getName()))
                .map(skillsByRequestType::get).orElse(null);
        if (skillOperation == null) {
            LOGGER.error(String.format(
                    "Skill operation identified by namespace (%s) and/or name (%s) not found.",
                    Optional.ofNullable(request.getHeader())
                            .map(MessageHeader::getNamespace)
                            .orElse("N/A; header is null"),
                    Optional.ofNullable(request.getHeader()).map(MessageHeader::getName).orElse("N/A; header is null")
            ));
            return null;
        }
        Response<?> operationResponse = skillOperation.invoke(request, context);
        if (operationResponse == null) {
            LOGGER.warn("Skill operation returned null.");
            return null;
        }
        debug(operationResponse);
        return new BaseSkillResponse<>(new JacksonJsonMarshaller<>(), operationResponse);
    }

    private void debug(AbstractMessage<?> message) {
        String messageId = message.getHeader().getMessageId();
        try {
            LOGGER.debug(String.format("%s message %s:%n%s",
                    message.getHeader().getName(),
                    messageId,
                    jsonAdapter.writeValueAsString(message)));
        } catch (JsonProcessingException e) {
            LOGGER.warn(String.format("Failed to print message %s.", messageId), e);
        }
    }
}
