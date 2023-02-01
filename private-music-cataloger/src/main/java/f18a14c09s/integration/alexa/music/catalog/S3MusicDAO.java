package f18a14c09s.integration.alexa.music.catalog;


import f18a14c09s.integration.alexa.music.catalog.data.S3MediaFile;
import f18a14c09s.integration.alexa.music.catalog.data.S3MediaFolder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class S3MusicDAO {
    public static final String MP3EXT = ".mp3";
    public static final String JPGEXT = ".jpg";

    private String bucketName;
    private String s3Prefix;
    private S3Client s3Client;

    public S3MusicDAO(
            String bucketName,
            String s3Prefix
    ) {
        this.bucketName = bucketName;
        this.s3Prefix = s3Prefix;
        this.s3Client = S3Client.create();
    }

    public S3MediaFolder recurseMediaFolder() {
        return recurseMediaFolder(this.s3Prefix, 0);
    }

    private S3MediaFolder recurseMediaFolder(String s3Prefix, int level) {
        List<S3MediaFolder> children = new ArrayList<>();
        List<CommonPrefix> commonPrefixes = new ArrayList<>();
        List<S3Object> s3Objects = new ArrayList<>();
        for (ListObjectsV2Response response : s3Client.listObjectsV2Paginator(
                ListObjectsV2Request.builder().bucket(
                        bucketName
                ).delimiter(
                        "/" // DO NOT forget this!
                ).prefix(
                        s3Prefix
                ).build()
        )) {
            commonPrefixes.addAll(response.commonPrefixes());
            s3Objects.addAll(response.contents());
        }
        List<S3MediaFile> mp3s = filterMusicFiles(s3Objects);
        if (!mp3s.isEmpty()) {
            commonPrefixes.clear();
        }
        for (CommonPrefix subdir : commonPrefixes) {
            S3MediaFolder child = recurseMediaFolder(subdir.prefix(), level + 1);
            children.add(child);
        }
        return S3MediaFolder.of(
                s3Prefix,
                removeRootS3Prefix(s3Prefix),
                children,
                mp3s,
                filterAlbumArt(s3Objects)
        );
    }

    private boolean isAlbumArt(S3Object s3Object) {
        String[] pathSegments = s3Object.key().split("/");
        String filename = pathSegments[pathSegments.length - 1];
        return (filename.startsWith("ALBUM~") || filename.startsWith("AlbumArt")) && filename.endsWith(JPGEXT);
    }

    private String removeRootS3Prefix(String s3PrefixOrKey) {
        return s3PrefixOrKey.replaceAll("^" + Pattern.quote(s3Prefix), "");
    }

    private List<S3MediaFile> filterMusicFiles(List<S3Object> s3Objects) {
        List<S3MediaFile> musicMediaFiles = new ArrayList<>();
        List<S3Object> mp3Objects = s3Objects.stream()
                .filter(s3Object -> s3Object.key().toLowerCase().endsWith(MP3EXT))
                .collect(Collectors.toList());
        for (S3Object mp3Object : mp3Objects) {
            musicMediaFiles.add(S3MediaFile.of(
                    mp3Object.key(),
                    removeRootS3Prefix(mp3Object.key())
            ));
        }
        return musicMediaFiles;
    }

    private List<S3MediaFile> filterAlbumArt(List<S3Object> s3Objects) {
        List<S3MediaFile> albumArtMediaFiles = new ArrayList<>();
        List<S3Object> jpgObjects = s3Objects.stream()
                .filter(this::isAlbumArt).collect(Collectors.toList());
        for (S3Object jpgObject : jpgObjects) {
            albumArtMediaFiles.add(S3MediaFile.of(
                    jpgObject.key(),
                    removeRootS3Prefix(jpgObject.key())
            ));
        }
        return albumArtMediaFiles;
    }
}
