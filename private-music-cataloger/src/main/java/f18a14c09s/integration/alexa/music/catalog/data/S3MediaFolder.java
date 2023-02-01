package f18a14c09s.integration.alexa.music.catalog.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter(AccessLevel.PRIVATE)
public class S3MediaFolder {
    private String s3Key;
    private String path;
    private List<S3MediaFolder> subfolders;
    private List<S3MediaFile> musicFiles;
    private List<S3MediaFile> artFiles;

    public static S3MediaFolder of(
            String s3Key,
            String path,
            List<S3MediaFolder> subfolders,
            List<S3MediaFile> musicFiles,
            List<S3MediaFile> artFiles
    ) {
        S3MediaFolder folder = new S3MediaFolder();
        folder.setS3Key(s3Key);
        folder.setPath(path);
        folder.setSubfolders(subfolders);
        folder.setMusicFiles(musicFiles);
        folder.setArtFiles(artFiles);
        return folder;
    }
}
