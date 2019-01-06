package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.AlexaMediaPlayQueue;
import f18a14c09s.integration.alexa.music.messagetypes.GetItemRequest;
import f18a14c09s.integration.alexa.music.messagetypes.GetItemResponse;

import static f18a14c09s.integration.alexa.music.messagetypes.AlexaMediaPlayQueue.GET_ITEM;

public class GetItemSkill extends AbstractMusicSkill<GetItemRequest, GetItemResponse> {
    public GetItemSkill() {
        super(GetItemRequest.class, new RequestType(AlexaMediaPlayQueue.NAMESPACE_NAME, GET_ITEM.getMyName()));
    }

    @Override
    protected GetItemResponse invokeImpl(GetItemRequest request, Object context) {
        return null;
    }
}
