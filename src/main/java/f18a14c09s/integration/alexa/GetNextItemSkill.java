package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.messagetypes.GetNextItemRequest;
import f18a14c09s.integration.alexa.music.messagetypes.GetNextItemResponse;

public class GetNextItemSkill extends AbstractMusicSkill<GetNextItemRequest, GetNextItemResponse> {
    public GetNextItemSkill() {
        super(GetNextItemRequest.class, new RequestType("Alexa.Audio.PlayQueue", "GetNextItem"));
    }

    @Override
    protected GetNextItemResponse invokeImpl(GetNextItemRequest request, Object context) {
        return null;
    }
}
