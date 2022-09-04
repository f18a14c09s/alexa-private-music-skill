package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.entities.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CrossReferenceOldEntityIdsCLI {
    private static final DynamoDBCatalogDAO CATALOG_DAO = new DynamoDBCatalogDAO(
            "private-music-alexa-skill-StringPartitionKeyStringSortKeyTable",
            "private-music-alexa-skill-StringPartitionKeyNumericSortKeyTable"
    );

    private static final class MusicEntityKeyExtractors extends HashMap<Class<? extends BaseEntity>, Function<? extends BaseEntity, Object>> {
        public <E extends BaseEntity> MusicEntityKeyExtractors withExtractor(
                Class<E> entityClass,
                Function<E, Object> extractor
        ) {
            this.put(entityClass, extractor);
            return this;
        }
    }

    private static final MusicEntityKeyExtractors MUSIC_ENTITY_KEY_EXTRACTORS = new MusicEntityKeyExtractors().withExtractor(
            Track.class,
            Track::getUrl
    ).withExtractor(
            Album.class,
            album -> List.of(
                    Optional.ofNullable(album.getArtists()).map(List::iterator).filter(Iterator::hasNext).map(Iterator::next).map(BaseEntityReference::getNames).map(List::iterator).filter(Iterator::hasNext).map(Iterator::next).map(EntityName::getValue).orElse("Unknown"),
                    Optional.ofNullable(album.getNames()).map(List::iterator).filter(Iterator::hasNext).map(Iterator::next).map(EntityName::getValue).orElse("Unknown")
            )
    ).withExtractor(
            Artist.class,
            artist -> Optional.of(artist).map(Artist::getNames).map(List::iterator).filter(Iterator::hasNext).map(Iterator::next).map(EntityName::getValue).orElse("Unknown")
    );

    public static void main(String... args) {
        System.out.println(CATALOG_DAO.listTrackDeletions().size());
        System.out.println(CATALOG_DAO.listAlbumDeletions().size());
        System.out.println(CATALOG_DAO.listArtistDeletions().size());
    }
}
