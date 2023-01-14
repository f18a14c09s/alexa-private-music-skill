package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.util.UrlStandardization;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AudioStreamUrlBuilder {
    private Track track;
    private String secretKey;
    private Long timestamp;
    private Long nonce;

    public static AudioStreamUrlBuilder newInstance() {
        return new AudioStreamUrlBuilder();
    }

    public AudioStreamUrlBuilder withTrack(Track track) {
        this.track = track;
        return this;
    }

    public AudioStreamUrlBuilder withSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public AudioStreamUrlBuilder withTimestamp() {
        this.timestamp = System.currentTimeMillis();
        return this;
    }

    public AudioStreamUrlBuilder withNonce() {
        this.nonce = new SecureRandom().longs(
                1,
                100000000000000L,
                1000000000000000L
        ).findAny().getAsLong();
        return this;
    }

    public String build() {
        final String properlyEncodedTrackUrl = UrlStandardization.encodeRawPathComponents(
                track.getUrl()
        );

        List<String> orderedUnsignedQuery = new ArrayList<>();

        if (timestamp != null) {
            orderedUnsignedQuery.add(String.format("ts=%s", timestamp));
        }
        if (nonce != null) {
            orderedUnsignedQuery.add(String.format("nc=%s", nonce));
        }

        String unsignedUrl = String.format(
                "%s%s%s",
                properlyEncodedTrackUrl,
                (orderedUnsignedQuery.isEmpty() ? "" : "?"),
                String.join("&", orderedUnsignedQuery)
        );

        byte[] hmacSignatureBytes;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            hmacSignatureBytes = mac.doFinal(unsignedUrl.getBytes(StandardCharsets.UTF_8));
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return String.format(
                "%s%ssg=%s",
                unsignedUrl,
                (orderedUnsignedQuery.isEmpty() ? "?" : "&"),
                URLEncoder.encode(
                        Base64.getEncoder().encodeToString(hmacSignatureBytes),
                        StandardCharsets.UTF_8
                )
        );
    }
}
