package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.Track;

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
            return dao.findArtistFirstTrack(contentId);
        } else if (entity instanceof Album) {
            return dao.findAlbumFirstTrack(contentId);
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
}
