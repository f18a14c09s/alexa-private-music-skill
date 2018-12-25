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
    private String dateString;
    private String copyright;
    private String comment;
    private Path filePath;
    private Long trackNumber;

    public TrackMetadata() {
    }

    public TrackMetadata(String title,
                         String author,
                         String album,
                         Long durationSeconds,
                         String dateString,
                         String copyright,
                         String comment,
                         Long trackNumber) {
        this.title = title;
        this.author = author;
        this.album = album;
        this.durationSeconds = durationSeconds;
        this.dateString = dateString;
        this.copyright = copyright;
        this.comment = comment;
        this.trackNumber = trackNumber;
    }
}
