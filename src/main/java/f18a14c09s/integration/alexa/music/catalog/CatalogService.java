package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.control.data.AdjustItemControl;
import f18a14c09s.integration.alexa.music.control.data.CommandItemControl;
import f18a14c09s.integration.alexa.music.data.Item;
import f18a14c09s.integration.alexa.music.data.ItemRules;
import f18a14c09s.integration.alexa.music.data.Stream;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.alexa.music.playback.data.PlaybackInfo;

import java.io.IOException;
import java.util.*;

public class CatalogService {
    private CatalogDAO dao;

    public CatalogService() throws IOException {
        this.dao = new CatalogDAO();
    }

    public Item getFirstTrack(String contentId) {
        BaseEntity entity = dao.findEntity(BaseEntity.class, contentId);
        if (entity instanceof Artist) {
            return findArtistFirstTrack(contentId);
        } else if (entity instanceof Album) {
            return findAlbumFirstTrack(contentId);
        } else if (entity instanceof Track) {
            return toItem(Collections.singletonList((Track) entity), 0);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Not sure how to get the first track for entity %s of type: %s.",
                    contentId,
                    Optional.ofNullable(entity)
                            .map(Object::getClass)
                            .map(Class::getName)
                            .orElse("Unknown, as the object is null")));
        }
    }

    public Item getPreviousTrack(String contentId, String currentTrackId) {
        BaseEntity entity = dao.findEntity(BaseEntity.class, contentId);
        if (entity instanceof Artist) {
            return findArtistPreviousTrack(contentId, currentTrackId);
        } else if (entity instanceof Album) {
            return findAlbumPreviousTrack(contentId, currentTrackId);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Not sure how to get the previous track for entity %s of type: %s.",
                    contentId,
                    Optional.ofNullable(entity)
                            .map(Object::getClass)
                            .map(Class::getName)
                            .orElse("Unknown, as the object is null")));
        }
    }

    public Item getNextTrack(String contentId, String currentTrackId) {
        BaseEntity entity = dao.findEntity(BaseEntity.class, contentId);
        if (entity instanceof Artist) {
            return findArtistNextTrack(contentId, currentTrackId);
        } else if (entity instanceof Album) {
            return findAlbumNextTrack(contentId, currentTrackId);
        } else if (entity instanceof Track) {
            // Assumption is that null will be handled appropriately (e.g. the response will be "end-of-queue.").
            return null;
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Not sure how to get the next track for entity %s of type: %s.",
                    contentId,
                    Optional.ofNullable(entity)
                            .map(Object::getClass)
                            .map(Class::getName)
                            .orElse("Unknown, as the object is null")));
        }
    }

    private Item findArtistFirstTrack(String id) {
        List<Track> tracks = dao.findArtistTracks(id);
        return toItem(tracks, 0);
    }

    private Item toItem(List<Track> tracks, int j) {
        Track firstTrack = tracks.get(j);
        Item item = new Item();
        item.setControls(Arrays.asList(CommandItemControl.previous(j > 0),
                CommandItemControl.next(j + 1 < tracks.size()),
                AdjustItemControl.seekPosition(false)));
        item.setDurationInMilliseconds(Optional.ofNullable(firstTrack.getDurationSeconds())
                .map(seconds -> seconds * 1000L)
                .orElse(null));
        item.setId(firstTrack.getId());
        item.setMetadata(firstTrack.toMediaMetadata());
        item.setPlaybackInfo(PlaybackInfo.defaultType());
        item.setRules(ItemRules.disallowFeedback());
        Calendar validUntil = Calendar.getInstance();
        validUntil.add(Calendar.YEAR, 1);
        item.setStream(new Stream(firstTrack.getId(), firstTrack.getUrl(), 0L, validUntil));
        return item;
    }

    private Item extractPreviousTrack(String currentTrackId, List<Track> tracks) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId().equals(currentTrackId)) {
                return toItem(tracks, i - 1);
            }
        }
        return null;
    }

    private Item extractNextTrack(String currentTrackId, List<Track> tracks) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId().equals(currentTrackId)) {
                return toItem(tracks, i + 1);
            }
        }
        return null;
    }

    private Item findArtistPreviousTrack(String artistId, String currentTrackId) {
        List<Track> tracks = dao.findArtistTracks(artistId);
        return extractPreviousTrack(currentTrackId, tracks);
    }

    private Item findArtistNextTrack(String artistId, String currentTrackId) {
        List<Track> tracks = dao.findArtistTracks(artistId);
        return extractNextTrack(currentTrackId, tracks);
    }

    private Item findAlbumFirstTrack(String id) {
        List<Track> tracks = dao.findAlbumTracks(id);
        return toItem(tracks, 0);
    }

    private Item findAlbumPreviousTrack(String albumId, String currentTrackId) {
        List<Track> tracks = dao.findAlbumTracks(albumId);
        return extractPreviousTrack(currentTrackId, tracks);
    }

    private Item findAlbumNextTrack(String albumId, String currentTrackId) {
        List<Track> tracks = dao.findAlbumTracks(albumId);
        return extractNextTrack(currentTrackId, tracks);
    }
}
