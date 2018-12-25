package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.data.AbstractMessage;
import f18a14c09s.integration.alexa.music.catalog.CatalogDAO;
import f18a14c09s.integration.alexa.music.catalog.CatalogService;
import f18a14c09s.integration.alexa.music.control.data.AdjustItemControl;
import f18a14c09s.integration.alexa.music.control.data.CommandItemControl;
import f18a14c09s.integration.alexa.music.control.data.ToggleQueueControl;
import f18a14c09s.integration.alexa.music.data.*;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.alexa.music.messagetypes.AlexaMediaPlayback;
import f18a14c09s.integration.alexa.music.messagetypes.InitiateRequest;
import f18a14c09s.integration.alexa.music.messagetypes.InitiateResponse;
import f18a14c09s.integration.alexa.music.playback.data.PlaybackInfo;

import java.io.IOException;
import java.util.*;

public class InitiationSkill extends AbstractMusicSkill<InitiateRequest, InitiateResponse> {
    private CatalogDAO dao;
    private CatalogService catalogService;

    public InitiationSkill() {
        super(InitiateRequest.class,
                new RequestType(AlexaMediaPlayback.NAMESPACE_NAME, AlexaMediaPlayback.INITIATE.getMyName()));
        try {
            dao = new CatalogDAO();
            catalogService = new CatalogService();
        } catch (IOException e) {
            getLogger().error("Unable to setup catalog DAO and/or service.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected InitiateResponse invokeImpl(InitiateRequest request, Object context) {
        Track firstTrack = Optional.ofNullable(request)
                .map(AbstractMessage::getPayload)
                .map(InitiateRequest.Payload::getContentId)
                .map(catalogService::getFirstTrack)
                .orElse(null);
        AudioPlayerQueue audioPlayerQueue = new AudioPlayerQueue();
        Item item = new Item();
        item.setControls(Arrays.asList(CommandItemControl.previous(firstTrack.getFirst()
                        .map(isFirst -> !isFirst)
                        .orElse(false)),
                CommandItemControl.next(firstTrack.getLast().map(isLast -> !isLast).orElse(false)),
                AdjustItemControl.seekPosition(false)));
        item.setDurationInMilliseconds(Optional.ofNullable(firstTrack.getDurationSeconds())
                .map(seconds -> seconds * 1000L)
                .orElse(null));
//        item.setFeedback();
        item.setId(firstTrack.getId());
        item.setMetadata(firstTrack.toMediaMetadata());
        item.setPlaybackInfo(PlaybackInfo.defaultType());
        item.setRules(ItemRules.disallowFeedback());
        Calendar validUntil = Calendar.getInstance();
        validUntil.add(Calendar.YEAR, 1);
        item.setStream(new Stream(firstTrack.getId(), firstTrack.getUrl(), 0L, validUntil));
        audioPlayerQueue.setControls(Arrays.asList(ToggleQueueControl.shuffle(false, false),
                ToggleQueueControl.loop(false, false)));
        audioPlayerQueue.setFirstItem(item);
        audioPlayerQueue.setId(UUID.randomUUID().toString());
//        audioPlayerQueue.setQueueIdsToDeactivate();
        audioPlayerQueue.setRules(QueueRules.disallowFeedback());
        return new InitiateResponse(request.getHeader().getMessageId(), audioPlayerQueue);
    }
}
