package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.AlexaAudioPlayQueue;
import f18a14c09s.integration.alexa.music.messagetypes.GetPreviousItemRequest;
import f18a14c09s.integration.alexa.music.messagetypes.GetPreviousItemResponse;

public class GetPreviousItemSkill extends AbstractMusicSkill<GetPreviousItemRequest, GetPreviousItemResponse> {
    public GetPreviousItemSkill() {
        super(GetPreviousItemRequest.class,
                new RequestType(AlexaAudioPlayQueue.NAMESPACE_NAME, AlexaAudioPlayQueue.GET_PREVIOUS_ITEM.getMyName()));
    }

    @Override
    protected GetPreviousItemResponse invokeImpl(GetPreviousItemRequest request, Object context) {
        GetPreviousItemResponse retval = new GetPreviousItemResponse(request.getHeader().getMessageId(),
                getCatalogService().getPreviousTrack(request.getPayload().getCurrentItemReference().getContentId(),
                        request.getPayload().getCurrentItemReference().getId()));
        return retval;
    }
}
