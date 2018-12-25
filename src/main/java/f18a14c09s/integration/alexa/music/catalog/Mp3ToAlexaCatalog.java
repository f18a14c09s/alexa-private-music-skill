package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.entities.EntityName;
import f18a14c09s.integration.alexa.music.entities.Popularity;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.mp3.TrackMetadata;

import java.util.*;

public class Mp3ToAlexaCatalog {

    private ArrayList<String> defaultContentLanguages() {
        return asArrayList("en");
    }

    private <E> ArrayList<E> asArrayList(E... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    public Track mp3ToTrackEntity(TrackMetadata mp3) {
        Track retval = new Track();
        retval.setLanguageOfContent(defaultContentLanguages());
        retval.setNames(asArrayList(new EntityName("en", mp3.getTitle())));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setReleaseType("Studio Recording");
        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setId(UUID.randomUUID().toString());
        return retval;
    }
}
