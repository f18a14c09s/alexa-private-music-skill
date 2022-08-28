package f18a14c09s.integration.alexa.music.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import f18a14c09s.integration.alexa.music.catalog.data.*;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.Track;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;


public class DynamoDBCatalogDAO {
    private String stringPkStringSkTableName;
    private String stringPkNumericSkTableName;
    private DynamoDbEnhancedClient dynamodbClient;

    public DynamoDBCatalogDAO(
            String stringPkStringSkTableName,
            String stringPkNumericSkTableName
    ) {
        this.dynamodbClient = DynamoDbEnhancedClient.builder().build();
        this.stringPkStringSkTableName = stringPkStringSkTableName;
        this.stringPkNumericSkTableName = stringPkNumericSkTableName;
    }

    public void save(Track musicEntity) {
        saveMusicEntity(musicEntity, new TrackItem(), TrackItem.class);
    }

    public void save(Album musicEntity) {
        saveMusicEntity(musicEntity, new AlbumItem(), AlbumItem.class);
    }

    public void save(Artist musicEntity) {
        saveMusicEntity(musicEntity, new ArtistItem(), ArtistItem.class);
    }

    public void saveChildTrackAssociations(
            Class<? extends BaseEntity> musicEntityClass,
            String entityId,
            List<Track> presortedChildTracks
    ) {
        for (int i = 0; i < presortedChildTracks.size(); i++) {
            ChildTrackItem dynamodbItem = new ChildTrackItem();
            dynamodbItem.setPk(
                    ChildTrackItem.formatPartitionKey(
                            musicEntityClass,
                            entityId
                    )
            );
            dynamodbItem.setSk(i);
            dynamodbItem.setTrackId(presortedChildTracks.get(i).getId());
            saveEntityWithNumericSortKey(dynamodbItem, ChildTrackItem.class);
        }
    }

    private static String formatMusicEntityPartitionKey(
            Class<? extends BaseEntity> clazz
    ) {
        return String.format(
                "MUSICENTITYTYPE=%s",
                clazz.getSimpleName()
        );
    }

    private static String formatMusicEntitySortKey(
            String id
    ) {
        return String.format(
                "ENTITYID=%s,LISTTYPE=MUSICENTITIES",
                id
        );
    }

    private <E extends BaseEntity, DE extends AbstractMusicEntityItem<E>> void saveMusicEntity(E entity, DE dynamoDbItem, Class<DE> clazz) {
        dynamoDbItem.setEntity(entity);
        dynamoDbItem.setPk(formatMusicEntityPartitionKey(entity.getClass()));
        dynamoDbItem.setSk(formatMusicEntitySortKey(entity.getId()));
        saveEntityWithStringSortKey(dynamoDbItem, clazz);
    }

    private <E extends Object> void saveEntityWithStringSortKey(E entity, Class<E> clazz) {
        DynamoDbTable<E> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkStringSkTableName, TableSchema.fromBean(clazz)
        );
        catalogTableWithEntitySpecificSchema.putItem(entity);
    }

    private <E extends Object> void saveEntityWithNumericSortKey(E entity, Class<E> clazz) {
        DynamoDbTable<E> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkNumericSkTableName, TableSchema.fromBean(clazz)
        );
        catalogTableWithEntitySpecificSchema.putItem(entity);
    }

    public Track findTrack(String id) {
        return findMusicEntity(
                Track.class, id, new TrackItem(), TrackItem.class
        );
    }

    public Album findAlbum(String id) {
        return findMusicEntity(
                Album.class, id, new AlbumItem(), AlbumItem.class
        );
    }

    public Artist findArtist(String id) {
        return findMusicEntity(
                Artist.class, id, new ArtistItem(), ArtistItem.class
        );
    }

    private <E extends BaseEntity, DE extends AbstractMusicEntityItem<E>> E findMusicEntity(
            Class<E> clazz, String id, DE keyHolder, Class<DE> dynamodbItemClass
    ) {
        DynamoDbTable<DE> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkStringSkTableName, TableSchema.fromBean(dynamodbItemClass)
        );
        keyHolder.setPk(formatMusicEntityPartitionKey(clazz));
        keyHolder.setSk(formatMusicEntitySortKey(id));
        DE actualItem = catalogTableWithEntitySpecificSchema.getItem(
                keyHolder
        );
        return actualItem == null ? null : actualItem.getEntity();
    }

    public Track getFirstChildTrack(
            Class<? extends BaseEntity> parentEntityClass,
            String parentEntityId
    ) {
        PageIterable<ChildTrackItem> childTracks = iterateChildTracks(
                parentEntityClass, parentEntityId
        );
        ChildTrackItem firstChildItem = childTracks.stream().limit(
                1
        ).map(
                Page::items
        ).flatMap(
                List::stream
        ).findFirst().orElse(null);
        if (firstChildItem == null) {
            return null;
        }
        return findTrack(
                firstChildItem.getTrackId()
        );
    }

    public Track getPreviousChildTrack(
            Class<? extends BaseEntity> parentEntityClass,
            String parentEntityId,
            String currentTrackId
    ) {
        PageIterable<ChildTrackItem> childTracks = iterateChildTracks(
                parentEntityClass, parentEntityId
        );
        boolean currentTrackFound = false;
        ChildTrackItem previousChildItem = null;
        for (Page<ChildTrackItem> page : childTracks) {
            for (ChildTrackItem childTrackItem : page.items()) {
                if (childTrackItem.getTrackId().equals(currentTrackId)) {
                    currentTrackFound = true;
                    break;
                }
                previousChildItem = childTrackItem;
            }
            if (currentTrackFound) {
                break;
            }
        }
        if (!currentTrackFound) {
            return null;
        }
        return findTrack(
                previousChildItem.getTrackId()
        );
    }

    public Track getNextChildTrack(
            Class<? extends BaseEntity> parentEntityClass,
            String parentEntityId,
            String currentTrackId
    ) {
        PageIterable<ChildTrackItem> childTracks = iterateChildTracks(
                parentEntityClass, parentEntityId
        );
        boolean currentTrackFound = false;
        ChildTrackItem nextChildItem = null;
        for (Page<ChildTrackItem> page : childTracks) {
            for (ChildTrackItem childTrackItem : page.items()) {
                if (currentTrackFound) {
                    nextChildItem = childTrackItem;
                    break;
                } else if (childTrackItem.getTrackId().equals(currentTrackId)) {
                    currentTrackFound = true;
                }
            }
            if (nextChildItem != null) {
                break;
            }
        }
        if (nextChildItem == null) {
            return null;
        }
        return findTrack(
                nextChildItem.getTrackId()
        );
    }

    private PageIterable<ChildTrackItem> iterateChildTracks(
            Class<? extends BaseEntity> parentEntityClass,
            String parentEntityId
    ) {
        DynamoDbTable<ChildTrackItem> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkNumericSkTableName, TableSchema.fromBean(ChildTrackItem.class)
        );
        String partitionKey = ChildTrackItem.formatPartitionKey(
                parentEntityClass,
                parentEntityId
        );
        return catalogTableWithEntitySpecificSchema.query(
                QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(
                                partitionKey
                        ).build()
                )
        );
    }

    // GetPlayableContent:
    // Get music entity (e.g. artist, album, track) by type and ID.
    // Table with string partition key; String sort key:
    //     MUSICENTITYTYPE=...; ENTITYID=...,LISTTYPE=MUSICENTITIES
    //
    // Initiate:
    // Either list and sort all of the artist/album's tracks or find the artist/album's first track.
    // Table with string partition key; numeric sort key:
    //     MUSICENTITYTYPE=...,ENTITYID=...,LISTTYPE=CHILDTRACKS; 123
    // Return the first item.
    //
    // GetPreviousItem:
    // Either list and sort all of the artist/album's tracks or find the artist/album's track immediately preceding current track.
    // Same DynamoDB query as Initiate.
    // Iterate the result list until the current track is found, then return the previous item if it exists.
    //
    // GetNextItem:
    // Either list and sort all of the artist/album's tracks or find the artist/album's track immediately following current track.
    // Same DynamoDB query as Initiate.
    // Iterate the result list until the current track is found, then return the next item if it exists.
}
