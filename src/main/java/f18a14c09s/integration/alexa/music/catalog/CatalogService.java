package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.data.AbstractAudioQueue;
import f18a14c09s.integration.alexa.music.data.AlbumAudioQueue;
import f18a14c09s.integration.alexa.music.data.ArtistAudioQueue;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.Track;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.util.*;

public class CatalogService {
    private CatalogDAO dao;

    public CatalogService() throws IOException {
        this.dao = new CatalogDAO();
    }

    public Track getFirstTrack(String contentId) {
        BaseEntity entity = dao.findEntity(BaseEntity.class, contentId);
        if (entity instanceof Artist) {
            return findArtistFirstTrack(contentId);
        } else if (entity instanceof Album) {
            return findAlbumFirstTrack(contentId);
        } else if (entity instanceof Track) {
            return (Track) entity;
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

    private Track findArtistFirstTrack(String id) {
        List<Track> tracks;
        try {
            ArtistAudioQueue queue = dao.findArtistQueue(id);
            tracks = queue.getTracks();
        } catch (NoResultException e) {
            Artist artist = dao.findEntity(Artist.class, id);
            tracks = dao.findArtistTracks(id);
            ArtistAudioQueue queue = new ArtistAudioQueue();
            queue.setArtist(artist);
            queue.setTracks(tracks);
            dao.save(queue);
        }
        return tracks.isEmpty() ? null : tracks.get(0);
    }

    private Track extractPreviousTrack(String currentTrackId, AbstractAudioQueue queue) {
        for (int i = 0; i < queue.getTracks().size(); i++) {
            if (queue.getTracks().get(i).getId().equals(currentTrackId)) {
                return queue.getTracks().get(i - 1);
            }
        }
        return null;
    }

    private Track extractNextTrack(String currentTrackId, AbstractAudioQueue queue) {
        for (int i = 0; i < queue.getTracks().size(); i++) {
            if (queue.getTracks().get(i).getId().equals(currentTrackId)) {
                return queue.getTracks().get(i + 1);
            }
        }
        return null;
    }

    private Track findArtistPreviousTrack(String artistId, String currentTrackId) {
        ArtistAudioQueue queue = dao.findArtistQueue(artistId);
        return extractPreviousTrack(currentTrackId, queue);
    }

    private Track findArtistNextTrack(String artistId, String currentTrackId) {
        ArtistAudioQueue queue = dao.findArtistQueue(artistId);
        return extractNextTrack(currentTrackId, queue);
    }

    private Track findAlbumFirstTrack(String id) {
        List<Track> tracks;
        try {
            AlbumAudioQueue queue = dao.findAlbumQueue(id);
            tracks = queue.getTracks();
        } catch (NoResultException e) {
            Album album = dao.findEntity(Album.class, id);
            tracks = dao.findAlbumTracks(id);
            AlbumAudioQueue queue = new AlbumAudioQueue();
            queue.setAlbum(album);
            queue.setTracks(tracks);
            dao.save(queue);
        }
        return tracks.isEmpty() ? null : tracks.get(0);
    }

    private Track findAlbumPreviousTrack(String albumId, String currentTrackId) {
        AlbumAudioQueue queue = dao.findAlbumQueue(albumId);
        return extractPreviousTrack(currentTrackId, queue);
    }

    private Track findAlbumNextTrack(String albumId, String currentTrackId) {
        AlbumAudioQueue queue = dao.findAlbumQueue(albumId);
        return extractNextTrack(currentTrackId, queue);
    }
}
