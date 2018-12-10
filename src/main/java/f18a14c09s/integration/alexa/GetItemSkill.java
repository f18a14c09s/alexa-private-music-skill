package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.GetItemRequest;
import f18a14c09s.integration.alexa.music.messagetypes.GetItemResponse;

public class GetItemSkill extends AbstractMusicSkill<GetItemRequest, GetItemResponse> {
    public GetItemSkill() {
        super(GetItemRequest.class, new RequestType("Alexa.Media.PlayQueue", "GetItem"));
    }

    @Override
    protected GetItemResponse invokeImpl(GetItemRequest request, Object context) {
        return null;
    }
}
