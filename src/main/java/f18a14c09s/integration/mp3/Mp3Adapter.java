package f18a14c09s.integration.mp3;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.*;
import java.util.function.*;

public class Mp3Adapter {
    public TrackMetadata parseMetadata(InputStream mp3Stream) throws
            IOException,
            InvalidAudioFrameException,
            TagException,
            ReadOnlyFileException {
        File tempFile = File.createTempFile("track_", ".mp3");
        try (BufferedInputStream bis = new BufferedInputStream(mp3Stream);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            for (int b = bis.read(); b >= 0; b = bis.read()) {
                fos.write(b);
            }
            fos.flush();
        }
        tempFile.deleteOnExit();
        MP3File mp3File = new MP3File(tempFile);
        Optional<MP3AudioHeader> mp3AudioHeader = Optional.ofNullable(mp3File.getMP3AudioHeader());
        Optional<ID3v1Tag> id3v1Tag = Optional.ofNullable(mp3File.getID3v1Tag());
        Optional<AbstractID3v2Tag> id3v2Tag = Optional.ofNullable(mp3File.getID3v2Tag());
        Function<FieldKey, String> getFirst = fieldKey -> id3v2Tag.map(tag -> tag.getFirst(fieldKey))
                .filter(Mp3Adapter::notEmpty)
                .orElse(id3v1Tag.map(tag -> tag.getFirst(fieldKey)).filter(Mp3Adapter::notEmpty).orElse(null));
        return new TrackMetadata(getFirst.apply(FieldKey.TITLE),
                Optional.ofNullable(getFirst.apply(FieldKey.ARTIST)).orElse(getFirst.apply(FieldKey.ALBUM_ARTIST)),
                getFirst.apply(FieldKey.ALBUM),
                mp3AudioHeader.map(MP3AudioHeader::getTrackLength).map(Integer::longValue).orElse(null),
                getFirst.apply(FieldKey.YEAR),
                null,
//                (String) properties.get(Mp3AudioFileFormatParameter.copyright.name()),
                getFirst.apply(FieldKey.COMMENT),
                Optional.ofNullable(getFirst.apply(FieldKey.TRACK)).map(Long::parseLong).orElse(null));
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private TrackMetadata parseMetadataOld(InputStream mp3Stream) throws IOException, UnsupportedAudioFileException {
        byte[] bytes;
        try (BufferedInputStream bis = new BufferedInputStream(mp3Stream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int b = bis.read(); b >= 0; b = bis.read()) {
                baos.write(b);
            }
            baos.flush();
            bytes = baos.toByteArray();
        }
        MpegAudioFileReader reader = new MpegAudioFileReader();
        MpegAudioFileFormat audioFileFormat;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            audioFileFormat = (MpegAudioFileFormat) reader.getAudioFileFormat(bais);
        }
        Map<String, Object> properties = audioFileFormat.properties();
        Long duration = Optional.ofNullable(properties.get(Mp3AudioFileFormatParameter.duration.name()))
                .map(Number.class::cast)
                .map(Number::longValue)
                .orElse(null);
        if (duration == null) {
            Double frameSize = Optional.ofNullable(audioFileFormat.getFrameLength())
                    .map(Number::doubleValue)
                    .filter(len -> len >= 1.0)
                    .orElse(Optional.ofNullable(audioFileFormat.getFormat())
                            .map(AudioFormat::getFrameSize)
                            .map(Number::doubleValue)
                            .filter(sz -> sz >= 1.0)
                            .orElse(Optional.ofNullable(properties.get(Mp3Parameter.MP3_FRAMESIZE_BYTES.getValue()))
                                    .map(Number.class::cast)
                                    .map(Number::doubleValue)
                                    .orElse(null)));
            Double frameRate = Optional.ofNullable(audioFileFormat.getFormat())
                    .map(AudioFormat::getFrameRate)
                    .filter(sz -> sz >= 1.0)
                    .map(Number::doubleValue)
                    .orElse(Optional.ofNullable(properties.get(Mp3Parameter.MP3_FRAMERATE_FPS.getValue()))
                            .map(Number.class::cast)
                            .map(Number::doubleValue)
                            .orElse(null));
            duration = frameRate == null || frameSize == null ?
                    null :
                    Double.valueOf(Integer.valueOf(bytes.length).doubleValue() / frameRate / frameSize).longValue();
        }
        return new TrackMetadata((String) properties.get(Mp3AudioFileFormatParameter.title.name()),
                (String) properties.get(Mp3AudioFileFormatParameter.author.name()),
                (String) properties.get(Mp3AudioFileFormatParameter.album.name()),
                duration,
                (String) properties.get(Mp3AudioFileFormatParameter.date.name()),
                (String) properties.get(Mp3AudioFileFormatParameter.copyright.name()),
                (String) properties.get(Mp3AudioFileFormatParameter.comment.name()));
    }
}
