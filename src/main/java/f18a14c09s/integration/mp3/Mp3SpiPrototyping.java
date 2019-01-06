package f18a14c09s.integration.mp3;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

public class Mp3SpiPrototyping {
    public static void main(String... args) throws
            IOException,
            UnsupportedAudioFileException,
            InvalidAudioFrameException,
            TagException,
            ReadOnlyFileException {
        File albumDir = new File(args[0]);
        File[] mp3Files = albumDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        Arrays.sort(mp3Files, (lhs, rhs) -> lhs.getName().compareToIgnoreCase(rhs.getName()));
        for (File mp3File : mp3Files) {
            MpegAudioFileReader reader = new MpegAudioFileReader();
            MpegAudioFileFormat audioFileFormat;
            try (FileInputStream mp3Stream = new FileInputStream(mp3File)) {
                audioFileFormat = (MpegAudioFileFormat) reader.getAudioFileFormat(mp3Stream);
            }
            Map<?, ?> properties = audioFileFormat.properties();
            System.out.printf("File %s Properties:%s%n",
                    mp3File.getName(),
                    properties.entrySet()
                            .stream()
                            .map(keyValuePair -> String.format("%n\t%s: %s",
                                    keyValuePair.getKey(),
                                    keyValuePair.getValue()))
                            .collect(Collectors.joining()));
        }
        StringBuilder sb = new StringBuilder();
        for (File mp3File : mp3Files) {
            MP3File mp3 = new MP3File(mp3File);
            ID3v1Tag id3v1Tag = mp3.getID3v1Tag();
            AbstractID3v2Tag id3v2Tag = mp3.getID3v2Tag();
            sb.append(String.format("File %s:%n\tDuration: %s%n\tID3v1 Fields:%s%n\tID3v2 Fields:%s%n",
                    mp3File.getName(),
                    mp3.getMP3AudioHeader().getTrackLength(),
                    Arrays.stream(FieldKey.values())
                            .map(fieldKey -> Arrays.asList(fieldKey, id3v1Tag.getFirst(fieldKey)))
                            .filter(keyValuePair -> Objects.nonNull(keyValuePair.get(1)) &&
                                    !keyValuePair.get(1).toString().isEmpty())
                            .map(keyValuePair -> {
                                return String.format("%n\t\t%s: %s", keyValuePair.get(0), keyValuePair.get(1));
                            })
                            .collect(Collectors.joining()),
                    Arrays.stream(FieldKey.values())
                            .map(fieldKey -> Arrays.asList(fieldKey, id3v2Tag.getFirst(fieldKey)))
                            .filter(keyValuePair -> Objects.nonNull(keyValuePair.get(1)) &&
                                    !keyValuePair.get(1).toString().isEmpty())
                            .map(keyValuePair -> {
                                return String.format("%n\t\t%s: %s", keyValuePair.get(0), keyValuePair.get(1));
                            })
                            .collect(Collectors.joining())));
        }
        System.out.print(sb.toString());
    }
}
