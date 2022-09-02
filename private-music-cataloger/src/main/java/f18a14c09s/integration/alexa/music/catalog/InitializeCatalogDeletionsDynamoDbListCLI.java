package f18a14c09s.integration.alexa.music.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import f18a14c09s.integration.alexa.music.catalog.data.*;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.util.StringObjectMap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class InitializeCatalogDeletionsDynamoDbListCLI {
    private static final Map<String, Class<? extends AbstractCatalog>> CATALOG_TYPE_MAP = Map.of(
            CatalogTypeName.AMAZON_MUSIC_GROUP,
            MusicGroupCatalog.class,
            CatalogTypeName.AMAZON_MUSIC_ALBUM,
            MusicAlbumCatalog.class,
            CatalogTypeName.AMAZON_MUSIC_RECORDING,
            MusicRecordingCatalog.class
    );
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

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

    private static void markForDeletion(BaseEntity entity) {
        if (entity instanceof Track) {
            CATALOG_DAO.markForDeletion(
                    (Track) entity,
                    AbstractDeletionItem.DeletionSource.OUTDATED_CATALOG_FILE
            );
        } else if (entity instanceof Album) {
            CATALOG_DAO.markForDeletion(
                    (Album) entity,
                    AbstractDeletionItem.DeletionSource.OUTDATED_CATALOG_FILE
            );
        } else if (entity instanceof Artist) {
            CATALOG_DAO.markForDeletion(
                    (Artist) entity,
                    AbstractDeletionItem.DeletionSource.OUTDATED_CATALOG_FILE
            );
        } else {
            throw new IllegalArgumentException(String.format(
                    "%s object specified.  Not sure how to mark %s objects for deletion.",
                    entity.getClass().getSimpleName(),
                    entity.getClass().getSimpleName()
            ));
        }
    }

    public static void main(String... args) throws IOException, JsonProcessingException {
        File sourceDirectory = new File("C:\\Users\\franc\\Downloads\\Alexa Private Music Skill");
        for (File catalogFile : Optional.ofNullable(sourceDirectory.listFiles()).orElse(new File[0])) {
            StringObjectMap catalogMap;
            try (FileReader catalogReader = new FileReader(catalogFile)) {
                catalogMap = JSON_MAPPER.readValue(catalogReader, StringObjectMap.class);
            }
            String catalogType = (String) catalogMap.get("type");
            Class<? extends AbstractCatalog> catalogClass = CATALOG_TYPE_MAP.get(catalogType);
            if (catalogClass == null) {
                throw new RuntimeException(String.format(
                        "Catalog type %s not recognized.",
                        catalogType
                ));
            }
            AbstractCatalog catalog;
            try (FileReader catalogReader = new FileReader(catalogFile)) {
                catalog = JSON_MAPPER.readValue(catalogReader, catalogClass);
            }
            for (BaseEntity musicEntity : catalog.getEntities()) {
                musicEntity.setDeleted(true);
                markForDeletion(musicEntity);
            }
        }
    }
}
