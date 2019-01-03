package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.AlexaAudioPlayQueue;
import f18a14c09s.integration.alexa.music.messagetypes.GetNextItemRequest;
import f18a14c09s.integration.alexa.music.messagetypes.GetNextItemResponse;

public class GetNextItemSkill extends AbstractMusicSkill<GetNextItemRequest, GetNextItemResponse> {
    public GetNextItemSkill() {
        super(GetNextItemRequest.class,
                new RequestType(AlexaAudioPlayQueue.NAMESPACE_NAME, AlexaAudioPlayQueue.GET_NEXT_ITEM.getMyName()));
    }

    @Override
    protected GetNextItemResponse invokeImpl(GetNextItemRequest request, Object context) {
        GetNextItemResponse retval = new GetNextItemResponse(request.getHeader().getMessageId(),
                getCatalogService().getNextTrack(request.getPayload().getCurrentItemReference().getContentId(),
                        request.getPayload().getCurrentItemReference().getId()));
        return retval;
    }
}
