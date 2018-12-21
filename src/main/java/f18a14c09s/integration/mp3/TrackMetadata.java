package f18a14c09s.integration.mp3;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class TrackMetadata {
    private Long duration;
    private String title;
    private String author;
    private String album;
    private String dateString;
    private String copyright;
    private String comment;
    private Path filePath;

    public TrackMetadata() {
    }

    public TrackMetadata(String title,
                         String author,
                         String album,
                         Long duration,
                         String dateString,
                         String copyright,
                         String comment) {
        this.title = title;
        this.author = author;
        this.album = album;
        this.duration = duration;
        this.dateString = dateString;
        this.copyright = copyright;
        this.comment = comment;
    }
}
