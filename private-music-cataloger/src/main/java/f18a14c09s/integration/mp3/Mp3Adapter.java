package f18a14c09s.integration.mp3;

import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.id3.AbstractID3Tag;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Mp3Adapter {
    public TrackMetadata parseMetadata(InputStream mp3Stream) throws
            IOException,
            InvalidAudioFrameException,
            TagException,
            ReadOnlyFileException {
        File tempFile = File.createTempFile("track_", ".mp3");
        tempFile.deleteOnExit();
        try {
            try (BufferedInputStream bis = new BufferedInputStream(mp3Stream);
                 FileOutputStream fos = new FileOutputStream(tempFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                for (int b = bis.read(); b >= 0; b = bis.read()) {
                    bos.write(b);
                }
            }
            return parseMetadata(tempFile);
        } finally {
            tempFile.delete();
        }
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
        Map<String, Set<String>> distinctArtistNames = new HashMap<>();
        for (Tag tag : java.util.stream.Stream.of(
                mp3File.getID3v1Tag(),
                mp3File.getID3v2Tag()
        ).filter(Objects::nonNull).collect(Collectors.toList())) {
            AbstractID3Tag id3Tag = (AbstractID3Tag) tag;
            for (FieldKey fieldKey : List.of(FieldKey.ARTIST, FieldKey.ALBUM_ARTIST)) {
                for (String artistName : Optional.of(fieldKey)
                        .map(tag::getFields)
                        .stream()
                        .flatMap(List::stream)
                        .filter(TagTextField.class::isInstance)
                        .map(TagTextField.class::cast)
                        .filter(Predicate.not(TagField::isEmpty))
                        .map(TagTextField::getContent)
                        .filter(Predicate.not(String::isEmpty))
                        .collect(Collectors.toList())) {
                    distinctArtistNames.computeIfAbsent(
                            artistName,
                            mapKey -> new HashSet<>()
                    ).add(String.format(
                            "%s-%s",
                            id3Tag.getIdentifier(),
                            fieldKey.name()
                    ));
                }
            }
        }
        return new TrackMetadata(getField.apply(FieldKey.TITLE),
                getField.apply(FieldKey.ALBUM),
                mp3AudioHeader.map(MP3AudioHeader::getTrackLength).map(Integer::longValue).orElse(null),
                getField.apply(FieldKey.YEAR),
                null,
                getField.apply(FieldKey.COMMENT),
                Optional.ofNullable(getField.apply(FieldKey.TRACK)).map(Long::parseLong).orElse(null),
                Optional.ofNullable(getField.apply(FieldKey.TRACK_TOTAL)).map(Long::parseLong).orElse(null),
                distinctArtistNames);
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty() && !s.equals("null");
    }
}
