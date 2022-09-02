package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.data.AbstractMessage;
import f18a14c09s.integration.alexa.music.control.data.ToggleQueueControl;
import f18a14c09s.integration.alexa.music.data.AudioPlayerQueue;
import f18a14c09s.integration.alexa.music.data.Item;
import f18a14c09s.integration.alexa.music.data.QueueRules;
import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.AlexaMediaPlayback;
import f18a14c09s.integration.alexa.music.messagetypes.InitiateRequest;
import f18a14c09s.integration.alexa.music.messagetypes.InitiateResponse;

import java.util.*;

public class InitiationSkill extends AbstractMusicSkill<InitiateRequest, InitiateResponse> {
    public InitiationSkill() {
        super(InitiateRequest.class,
                new RequestType(AlexaMediaPlayback.NAMESPACE_NAME, AlexaMediaPlayback.INITIATE.getMyName()));
    }

    @Override
    protected InitiateResponse invokeImpl(InitiateRequest request, Object context) {
        Item firstTrack = Optional.ofNullable(request)
                .map(AbstractMessage::getPayload)
                .map(InitiateRequest.Payload::getContentId)
                .map(getCatalogService()::getFirstTrack)
                .orElse(null);
        AudioPlayerQueue audioPlayerQueue = new AudioPlayerQueue();
        audioPlayerQueue.setControls(Arrays.asList(ToggleQueueControl.shuffle(false, false),
                ToggleQueueControl.loop(false, false)));
        audioPlayerQueue.setFirstItem(firstTrack);
        audioPlayerQueue.setId(UUID.randomUUID().toString());
        audioPlayerQueue.setRules(QueueRules.disallowFeedback());
        return new InitiateResponse(request.getHeader().getMessageId(), audioPlayerQueue);
    }
}
