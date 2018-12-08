package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.data.RequestMap;
import f18a14c09s.integration.alexa.data.ResponseMap;
import f18a14c09s.integration.alexa.music.data.MusicRequestType;

public class GetItemSkill extends AbstractMusicSkill<RequestMap, ResponseMap> {
    public GetItemSkill() {
        super(RequestMap.class, new MusicRequestType("Alexa.Media.PlayQueue", "GetItem"));
    }

    @Override
    protected ResponseMap invokeImpl(RequestMap request, Object context) {
        return null;
    }
}
