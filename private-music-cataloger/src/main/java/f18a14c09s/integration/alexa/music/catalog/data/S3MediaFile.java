package f18a14c09s.integration.alexa.music.catalog.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
public class S3MediaFile {
    private String s3Key;
    private String path;

    public static S3MediaFile of(
            String s3Key, String path
    ) {
        S3MediaFile mediaFile = new S3MediaFile();
        mediaFile.setS3Key(s3Key);
        mediaFile.setPath(path);
        return mediaFile;
    }
}
