package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.data.RequestMap;
import f18a14c09s.integration.alexa.data.ResponseMap;
import f18a14c09s.integration.alexa.music.data.MusicRequestType;

public class GetPreviousItemSkill extends AbstractMusicSkill<RequestMap, ResponseMap> {
    public GetPreviousItemSkill() {
        super(RequestMap.class, new MusicRequestType("Alexa.Audio.PlayQueue", "GetPreviousItem"));
    }

    @Override
    protected ResponseMap invokeImpl(RequestMap request, Object context) {
        return null;
    }
}
