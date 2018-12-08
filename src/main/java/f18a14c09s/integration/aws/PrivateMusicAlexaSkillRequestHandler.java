package f18a14c09s.integration.aws;

import com.amazon.ask.SkillStreamHandler;
import f18a14c09s.integration.alexa.PrivateMusicSkill;

public class PrivateMusicAlexaSkillRequestHandler extends SkillStreamHandler {
    public PrivateMusicAlexaSkillRequestHandler() {
        super(new PrivateMusicSkill());
    }
}
