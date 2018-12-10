package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.GetPreviousItemRequest;
import f18a14c09s.integration.alexa.music.messagetypes.GetPreviousItemResponse;

public class GetPreviousItemSkill extends AbstractMusicSkill<GetPreviousItemRequest, GetPreviousItemResponse> {
    public GetPreviousItemSkill() {
        super(GetPreviousItemRequest.class, new RequestType("Alexa.Audio.PlayQueue", "GetPreviousItem"));
    }

    @Override
    protected GetPreviousItemResponse invokeImpl(GetPreviousItemRequest request, Object context) {
        return null;
    }
}
