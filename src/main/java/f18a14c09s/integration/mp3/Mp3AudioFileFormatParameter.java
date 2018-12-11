package f18a14c09s.integration.mp3;

public enum Mp3AudioFileFormatParameter {/*
MP3 audio file format parameters. Some parameters might be unavailable. So availability test is required before reading any parameter.
AudioFileFormat parameters.
duration [Long], duration in microseconds.
title [String], Title of the stream.
author [String], Name of the artist of the stream.
album [String], Name of the album of the stream.
date [String], The date (year) of the recording or release of the stream.
copyright [String], Copyright message of the stream.
comment [String], Comment of the stream.
*/
    duration(Long.class),
    title(String.class),
    author(String.class),
    album(String.class),
    date(String.class),
    copyright(String.class),
    comment(String.class);
    private Class<?> targetClass;

    Mp3AudioFileFormatParameter(Class<?> targetClass) {
        this.targetClass = targetClass;
    }
}
