package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.messaging.data.InitiateRequest;
import f18a14c09s.integration.alexa.music.messaging.data.InitiateResponse;
import f18a14c09s.integration.alexa.music.data.MusicRequestType;

import java.io.IOException;

public class InitiationSkill extends AbstractMusicSkill<InitiateRequest, InitiateResponse> {
    public InitiationSkill() {
        super(InitiateRequest.class, new MusicRequestType("Alexa.Media.Playback", "Initiate"));
    }

    @Override
    protected InitiateResponse invokeImpl(InitiateRequest request, Object context) {
        try {
            return getJsonAdapter().readValueFromResource("/example-data/Initiate.Response-example.json",
                    InitiateResponse.class);
        } catch (IOException e) {
            getLogger().error("Can't read the response payload.", e);
            // TODO: Respond with an error.
            return null;
        }
    }
}
