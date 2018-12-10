package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.messagetypes.RequestMap;
import f18a14c09s.integration.alexa.music.messagetypes.ResponseMap;
import f18a14c09s.integration.alexa.music.data.RequestType;

public class GetItemSkill extends AbstractMusicSkill<RequestMap, ResponseMap> {
    public GetItemSkill() {
        super(RequestMap.class, new RequestType("Alexa.Media.PlayQueue", "GetItem"));
    }

    @Override
    protected ResponseMap invokeImpl(RequestMap request, Object context) {
        return null;
    }
}
