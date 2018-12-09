package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.music.data.GetPlayableContentRequest;
import f18a14c09s.integration.alexa.music.data.GetPlayableContentResponse;
import f18a14c09s.integration.alexa.music.data.MusicRequestType;

import java.io.IOException;

public class GetPlayableContentSkill extends AbstractMusicSkill<GetPlayableContentRequest, GetPlayableContentResponse> {
    public GetPlayableContentSkill() {
        super(GetPlayableContentRequest.class, new MusicRequestType("Alexa.Media.Search", "GetPlayableContent"));
    }

    @Override
    protected GetPlayableContentResponse invokeImpl(GetPlayableContentRequest request, Object context) {
        try {
            return getJsonAdapter().readValueFromResource("/example-data/GetPlayableContent.Response-example.json",
                    GetPlayableContentResponse.class);
        } catch (IOException e) {
            getLogger().error("Can't read the response payload.", e);
            // TODO: Respond with an error.
            return null;
        }
    }
}