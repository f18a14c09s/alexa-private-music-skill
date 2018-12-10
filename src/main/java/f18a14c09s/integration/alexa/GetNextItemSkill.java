package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.messaging.data.RequestMap;
import f18a14c09s.integration.alexa.music.messaging.data.ResponseMap;
import f18a14c09s.integration.alexa.music.data.MusicRequestType;

public class GetNextItemSkill extends AbstractMusicSkill<RequestMap, ResponseMap> {
    public GetNextItemSkill() {
        super(RequestMap.class, new MusicRequestType("Alexa.Audio.PlayQueue", "GetNextItem"));
    }

    @Override
    protected ResponseMap invokeImpl(RequestMap request, Object context) {
        return null;
    }
}
