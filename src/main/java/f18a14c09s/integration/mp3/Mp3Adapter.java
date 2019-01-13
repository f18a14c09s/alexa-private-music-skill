package f18a14c09s.integration.mp3;

import f18a14c09s.integration.mp3.data.TrackMetadata;
import f18a14c09s.integration.util.DateUtil;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

public class Mp3Adapter {

    private Logger logger = Logger.getLogger(getClass().getName());

    static {
        // For some reason, JAudioTagger prints a lot of INFO and WARNING messages; so the following attempts to reduce
        // the number:
        Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
    }

    public TrackMetadata parseMetadata(File file) throws
            IOException,
            InvalidAudioFrameException,
            TagException,
            ReadOnlyFileException {
        MP3File mp3File = new MP3File(file, MP3File.LOAD_ALL, true);
        Optional<MP3AudioHeader> mp3AudioHeader = Optional.ofNullable(mp3File.getMP3AudioHeader());
        Optional<ID3v1Tag> id3v1Tag = Optional.ofNullable(mp3File.getID3v1Tag());
        Optional<AbstractID3v2Tag> id3v2Tag = Optional.ofNullable(mp3File.getID3v2Tag());
        Function<FieldKey, String> getField = fieldKey -> id3v2Tag.map(tag -> tag.getFirst(fieldKey))
                .filter(Mp3Adapter::notEmpty)
                .orElse(id3v1Tag.map(tag -> tag.getFirst(fieldKey)).filter(Mp3Adapter::notEmpty).orElse(null));
        return new TrackMetadata(getField.apply(FieldKey.TITLE),
                Optional.ofNullable(getField.apply(FieldKey.ARTIST)).orElse(getField.apply(FieldKey.ALBUM_ARTIST)),
                getField.apply(FieldKey.ALBUM),
                mp3AudioHeader.map(MP3AudioHeader::getTrackLength).map(Integer::longValue).orElse(null),
                Optional.ofNullable(getField.apply(FieldKey.YEAR)).map(yearString -> {
                    if (!yearString.matches("^[0-9]+$")) {
                        try {
                            Date date = DateUtil.parseAsIso8601UtcSeconds(yearString);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            return Integer.toString(cal.get(Calendar.YEAR));
                        } catch (ParseException e) {
                            logger.warning(String.format("File %s contains invalid \"year\" string: %s.",
                                    file.getAbsolutePath(),
                                    yearString));
                        }
                        return null;
                    }
                    return yearString;
                }).map(Long::parseLong).orElse(null),
                null,
                getField.apply(FieldKey.COMMENT),
                Optional.ofNullable(getField.apply(FieldKey.TRACK)).map(Long::parseLong).orElse(null),
                Optional.ofNullable(getField.apply(FieldKey.TRACK_TOTAL)).map(Long::parseLong).orElse(null));
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty() && !s.equals("null");
    }

}
