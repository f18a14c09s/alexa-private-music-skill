package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.control.data.AdjustItemControl;
import f18a14c09s.integration.alexa.music.control.data.CommandItemControl;
import f18a14c09s.integration.alexa.music.data.Item;
import f18a14c09s.integration.alexa.music.data.ItemRules;
import f18a14c09s.integration.alexa.music.data.Stream;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.alexa.music.playback.data.PlaybackInfo;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CatalogService {
    private static String SECRET_KEY = SsmClient.create().getParameter(
        GetParameterRequest.builder().withDecryption(true).name(
                "/alexa-private-music-skill/access-control/secret-key"
        ).build()
    ).parameter().value();
    private DynamoDBCatalogDAO catalogDAO;

    public CatalogService() throws IOException {
        this.catalogDAO = new DynamoDBCatalogDAO();
    }

    public Item getFirstTrack(String contentId) {
        Track track;
        if (isArtistEntity(contentId)) {
            return findArtistFirstTrack(contentId);
        } else if (isAlbumEntity(contentId)) {
            return findAlbumFirstTrack(contentId);
        } else if ((track = catalogDAO.findTrack(contentId)) != null) {
            return toItem(Collections.singletonList(track), 0);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Not sure how to get the first track for entity %s.",
                    contentId
            ));
        }
    }

    public Item getPreviousTrack(String contentId, String currentTrackId) {
        if (isArtistEntity(contentId)) {
            return findArtistPreviousTrack(contentId, currentTrackId);
        } else if (isAlbumEntity(contentId)) {
            return findAlbumPreviousTrack(contentId, currentTrackId);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Not sure how to get the previous track for entity %s.",
                    contentId
            ));
        }
    }

    public Item getNextTrack(String contentId, String currentTrackId) {
        if (isArtistEntity(contentId)) {
            return findArtistNextTrack(contentId, currentTrackId);
        } else if (isAlbumEntity(contentId)) {
            return findAlbumNextTrack(contentId, currentTrackId);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Not sure how to get the next track for entity %s.",
                    contentId
            ));
        }
    }
    private static String encodeTrackUrl(String trackUrl) {
        Matcher urlPrefixMatcher = Pattern.compile(
                "^https://[^/]+/"
        ).matcher(trackUrl);
        List<MatchResult> urlPrefixMatches = urlPrefixMatcher.results().collect(Collectors.toList());

        if(urlPrefixMatches.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "URL %s does not start with https://<hostname>/.",
                            trackUrl
                    )
            );
        }

        String urlPrefix = urlPrefixMatches.get(0).group(0);
        String urlSuffix = urlPrefixMatcher.replaceFirst("");

        String[] urlPathComponents = urlSuffix.split("/");
        String encodedUrlPath = Arrays.stream(urlPathComponents).map(pathComponent -> URLEncoder.encode(pathComponent, StandardCharsets.UTF_8)).collect(Collectors.joining());

        return urlPrefix + encodedUrlPath;
    }

    private Item toItem(List<Track> tracks, int trackIndex) {
        if (trackIndex < 0 || trackIndex >= tracks.size()) {
            return null;
        }
        Track track = tracks.get(trackIndex);
        Item item = new Item();
        item.setControls(Arrays.asList(CommandItemControl.previous(trackIndex > 0),
                CommandItemControl.next(trackIndex + 1 < tracks.size()),
                AdjustItemControl.seekPosition(false)));
        item.setDurationInMilliseconds(Optional.ofNullable(track.getDurationSeconds())
                .map(seconds -> seconds * 1000L)
                .orElse(null));
        item.setId(track.getId());
        item.setMetadata(track.toMediaMetadata());
        item.setPlaybackInfo(PlaybackInfo.defaultType());
        item.setRules(ItemRules.disallowFeedback());
        Calendar validUntil = Calendar.getInstance();
        validUntil.add(Calendar.YEAR, 1);

        byte[] hmacSignatureBytes;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            hmacSignatureBytes = mac.doFinal(
                    track.getUrl().getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException|InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        final String url = String.format(
                "%s?hmac_signature=%s",
                encodeTrackUrl(track.getUrl()),
                URLEncoder.encode(
                        Base64.getEncoder().encodeToString(hmacSignatureBytes),
                        StandardCharsets.UTF_8
                )
        );
        item.setStream(new Stream(track.getId(), url, 0L, validUntil));
        return item;
    }

    private Item extractPreviousTrack(String currentTrackId, List<Track> tracks) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId().equals(currentTrackId)) {
                return toItem(tracks, i - 1);
            }
        }
        return null;
    }

    private Item extractNextTrack(String currentTrackId, List<Track> tracks) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId().equals(currentTrackId)) {
                return toItem(tracks, i + 1);
            }
        }
        return null;
    }

    private Item findArtistFirstTrack(String id) {
        List<Track> tracks = catalogDAO.listChildTracks(
                Artist.class,
                id
        );
        return toItem(tracks, 0);
    }

    private Item findArtistPreviousTrack(String id, String currentTrackId) {
        List<Track> tracks = catalogDAO.listChildTracks(
                Artist.class,
                id
        );
        return extractPreviousTrack(currentTrackId, tracks);
    }

    private Item findArtistNextTrack(String id, String currentTrackId) {
        List<Track> tracks = catalogDAO.listChildTracks(
                Artist.class,
                id
        );
        return extractNextTrack(currentTrackId, tracks);
    }

    private Item findAlbumFirstTrack(String id) {
        List<Track> tracks = catalogDAO.listChildTracks(
                Album.class,
                id
        );
        return toItem(tracks, 0);
    }

    private Item findAlbumPreviousTrack(String id, String currentTrackId) {
        List<Track> tracks = catalogDAO.listChildTracks(
                Album.class,
                id
        );
        return extractPreviousTrack(currentTrackId, tracks);
    }

    private Item findAlbumNextTrack(String id, String currentTrackId) {
        List<Track> tracks = catalogDAO.listChildTracks(
                Album.class,
                id
        );
        return extractNextTrack(currentTrackId, tracks);
    }

    private boolean isArtistEntity(String contentId) {
        return catalogDAO.findArtist(contentId) != null;
    }

    private boolean isAlbumEntity(String contentId) {
        return catalogDAO.findAlbum(contentId) != null;
    }
}
