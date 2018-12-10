package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.messagetypes.RequestMap;
import f18a14c09s.integration.alexa.music.messagetypes.ResponseMap;
import f18a14c09s.integration.alexa.music.data.RequestType;

public class GetPreviousItemSkill extends AbstractMusicSkill<RequestMap, ResponseMap> {
    public GetPreviousItemSkill() {
        super(RequestMap.class, new RequestType("Alexa.Audio.PlayQueue", "GetPreviousItem"));
    }

    @Override
    protected ResponseMap invokeImpl(RequestMap request, Object context) {
        return null;
    }
}
