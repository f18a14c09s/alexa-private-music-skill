package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.messagetypes.RequestMap;
import f18a14c09s.integration.alexa.music.messagetypes.ResponseMap;
import f18a14c09s.integration.alexa.music.data.RequestType;

public class GetNextItemSkill extends AbstractMusicSkill<RequestMap, ResponseMap> {
    public GetNextItemSkill() {
        super(RequestMap.class, new RequestType("Alexa.Audio.PlayQueue", "GetNextItem"));
    }

    @Override
    protected ResponseMap invokeImpl(RequestMap request, Object context) {
        return null;
    }
}
