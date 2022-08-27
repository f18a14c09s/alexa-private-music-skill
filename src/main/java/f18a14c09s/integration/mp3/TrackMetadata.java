package f18a14c09s.integration.mp3;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class TrackMetadata {
    private Long durationSeconds;
    private String title;
    private String author;
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
                         String author,
                         String album,
                         Long durationSeconds,
                         String year,
                         String copyright,
                         String comment,
                         Long trackNumber,
                         Long lastTrackNumber) {
        this.title = title;
        this.author = author;
        this.album = album;
        this.durationSeconds = durationSeconds;
        this.year = year;
        this.copyright = copyright;
        this.comment = comment;
        this.trackNumber = trackNumber;
        this.lastTrackNumber = lastTrackNumber;
    }
}
