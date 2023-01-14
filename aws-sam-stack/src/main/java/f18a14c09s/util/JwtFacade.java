package f18a14c09s.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;
import java.util.UUID;

public class JwtFacade {
    public static String createJwt(
            String targetUrl,
            String secretKey
    ) {
        return JWT.create()
                .withAudience(targetUrl)
                .withIssuer("Private Music Alexa Skill")
                .withSubject("Alexa User")
                .withIssuedAt(Instant.now())
                .withJWTId(UUID.randomUUID().toString())
                .sign(Algorithm.HMAC256(secretKey));
    }
}
