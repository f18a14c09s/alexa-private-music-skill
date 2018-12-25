package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.data.AbstractMessage;
import f18a14c09s.integration.alexa.music.catalog.CatalogDAO;
import f18a14c09s.integration.alexa.music.data.AudioPlayerQueue;
import f18a14c09s.integration.alexa.music.data.Item;
import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.data.Stream;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.alexa.music.messagetypes.InitiateRequest;
import f18a14c09s.integration.alexa.music.messagetypes.InitiateResponse;
import f18a14c09s.integration.alexa.music.playback.data.PlaybackInfo;
import f18a14c09s.integration.alexa.music.playback.data.PlaybackInfoType;

import java.io.IOException;
import java.util.*;

public class InitiationSkill extends AbstractMusicSkill<InitiateRequest, InitiateResponse> {
    private CatalogDAO dao;

    public InitiationSkill() {
        super(InitiateRequest.class, new RequestType("Alexa.Media.Playback", "Initiate"));
        try {
            dao = new CatalogDAO();
        } catch (IOException e) {
            getLogger().error("Unable to setup catalog DAO.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected InitiateResponse invokeImpl(InitiateRequest request, Object context) {
        BaseEntity entity = Optional.ofNullable(request)
                .map(AbstractMessage::getPayload)
                .map(InitiateRequest.Payload::getContentId)
                .map(contentId -> dao.findEntity(BaseEntity.class, contentId))
                .orElse(null);
        // TODO: Get the first song for other entity types.
        Track track = entity instanceof Track ? (Track) entity : null;
        AudioPlayerQueue audioPlayerQueue = new AudioPlayerQueue();
//        audioPlayerQueue.setControls();
        Item item = new Item();
//        item.setControls();
        item.setDurationInMilliseconds(Optional.ofNullable(track.getDurationSeconds())
                .map(seconds -> seconds * 1000L)
                .orElse(null));
//        item.setFeedback();
        item.setId(track.getId());
        item.setMetadata(track.toMediaMetadata());
        PlaybackInfo playbackInfo = new PlaybackInfo();
        playbackInfo.setType(PlaybackInfoType.DEFAULT);
        item.setPlaybackInfo(playbackInfo);
//        item.setRules();
        Stream stream = new Stream();
        stream.setId(track.getId());
        stream.setOffsetInMilliseconds(0L);
        stream.setUri(track.getUrl());
        Calendar validUntil = Calendar.getInstance();
        validUntil.add(Calendar.YEAR, 20);
        stream.setValidUntil(validUntil);
        item.setStream(stream);
        audioPlayerQueue.setFirstItem(item);
//        audioPlayerQueue.setId();
//        audioPlayerQueue.setQueueIdsToActivate();
//        audioPlayerQueue.setRules();
        InitiateResponse.Payload payload = new InitiateResponse.Payload();
        payload.setPlaybackMethod(audioPlayerQueue);
        InitiateResponse response = new InitiateResponse(request.getHeader().getMessageId());
        response.setPayload(payload);
        return response;
    }
}
