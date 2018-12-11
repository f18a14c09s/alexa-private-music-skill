package f18a14c09s.integration.mp3;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.*;

public class Mp3Adapter {
    public TrackMetadata parseMetadata(InputStream mp3Stream) throws IOException, UnsupportedAudioFileException {
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
//        properties.entrySet()
//                .forEach(prop -> System.out.printf("assertEquals(\"%s\", trackMetadata.get%s%s());%n",
//                        prop.getValue(),
//                        Character.toUpperCase(prop.getKey().charAt(0)),
//                        prop.getKey().substring(1)));
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
