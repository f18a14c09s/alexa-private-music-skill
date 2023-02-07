package f18a14c09s.integration.mp3;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static f18a14c09s.util.CollectionUtil.asArrayList;

@Getter
@Setter
@DynamoDbBean
public class TrackMetadata {
    private Long durationSeconds;
    private String title;
    private Map<String, Set<String>> distinctArtistNames;
    private String album;
    private String year;
    private String copyright;
    private String comment;
    private String filePath;
    private Long trackNumber;
    private Long lastTrackNumber;

    public TrackMetadata() {
    }

    public TrackMetadata(String title,
                         String album,
                         Long durationSeconds,
                         String year,
                         String copyright,
                         String comment,
                         Long trackNumber,
                         Long lastTrackNumber,
                         Map<String, Set<String>> distinctArtistNames) {
        this.title = title;
        this.album = album;
        this.durationSeconds = durationSeconds;
        this.year = year;
        this.copyright = copyright;
        this.comment = comment;
        this.trackNumber = trackNumber;
        this.lastTrackNumber = lastTrackNumber;
        this.distinctArtistNames = distinctArtistNames;
    }

    public ArrayList<String> buildAlbumKey(String trackArtistName) {
        return asArrayList(
                trackArtistName,
                getAlbum()
        );
    }
}
