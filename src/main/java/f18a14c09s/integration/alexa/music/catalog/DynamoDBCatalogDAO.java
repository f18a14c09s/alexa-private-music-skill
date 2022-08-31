package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.catalog.data.*;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.mp3.TrackMetadata;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * GetPlayableContent:
 * Get music entity (e.g. artist, album, track) by type and ID.
 * Table with string partition key; String sort key:
 * MUSICENTITYTYPE=...; ENTITYID=...,LISTTYPE=MUSICENTITIES
 * <p>
 * Initiate:
 * Either list and sort all of the artist/album's tracks or find the artist/album's first track.
 * Table with string partition key; numeric sort key:
 * MUSICENTITYTYPE=...,ENTITYID=...,LISTTYPE=CHILDTRACKS; 123
 * Return the first item.
 * <p>
 * GetPreviousItem:
 * Either list and sort all of the artist/album's tracks or find the artist/album's track immediately preceding current track.
 * Same DynamoDB query as Initiate.
 * Iterate the result list until the current track is found, then return the previous item if it exists.
 * <p>
 * GetNextItem:
 * Either list and sort all of the artist/album's tracks or find the artist/album's track immediately following current track.
 * Same DynamoDB query as Initiate.
 * Iterate the result list until the current track is found, then return the next item if it exists.
 */
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

    public DynamoDBCatalogDAO() {
        this(
                System.getenv("STR_PK_STR_SK_DYNAMODB_TABLE_NAME"),
                System.getenv("STR_PK_NUM_SK_DYNAMODB_TABLE_NAME")
        );
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

    public void save(TrackMetadata trackMetadata) {
        TrackMetadataItem dynamodbItem = new TrackMetadataItem();
        dynamodbItem.setPk(TrackMetadataItem.formatPartitionKey());
        dynamodbItem.setSk(TrackMetadataItem.formatSortKey(
                trackMetadata.getFilePath()
        ));
        dynamodbItem.setData(trackMetadata);
        saveEntityWithStringSortKey(
                dynamodbItem,
                TrackMetadataItem.class
        );
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

    public void markForDeletion(
            Track entity,
            AbstractDeletionItem.DeletionSource source
    ) {
        TrackDeletionItem deletion = new TrackDeletionItem();
        deletion.setPk(AbstractDeletionItem.formatPartitionKey());
        deletion.setSk(AbstractDeletionItem.formatSortKey(
                entity
        ));
        deletion.setEntity(entity);
        saveEntityWithStringSortKey(
                deletion,
                TrackDeletionItem.class
        );
    }

    public void markForDeletion(
            Album entity,
            AbstractDeletionItem.DeletionSource source
    ) {
        AlbumDeletionItem deletion = new AlbumDeletionItem();
        deletion.setPk(AbstractDeletionItem.formatPartitionKey());
        deletion.setSk(AbstractDeletionItem.formatSortKey(
                entity
        ));
        deletion.setEntity(entity);
        deletion.setDeletionSource(source);
        saveEntityWithStringSortKey(
                deletion,
                AlbumDeletionItem.class
        );
    }

    public void markForDeletion(
            Artist entity,
            AbstractDeletionItem.DeletionSource source
    ) {
        ArtistDeletionItem deletion = new ArtistDeletionItem();
        deletion.setPk(AbstractDeletionItem.formatPartitionKey());
        deletion.setSk(AbstractDeletionItem.formatSortKey(
                entity
        ));
        deletion.setEntity(entity);
        deletion.setDeletionSource(source);
        saveEntityWithStringSortKey(
                deletion,
                ArtistDeletionItem.class
        );
    }

    private <E extends BaseEntity, DE extends AbstractMusicEntityItem<E>> void saveMusicEntity(E entity, DE dynamoDbItem, Class<DE> clazz) {
        dynamoDbItem.setEntity(entity);
        dynamoDbItem.setPk(AbstractMusicEntityItem.formatPartitionKey(entity.getClass()));
        dynamoDbItem.setSk(AbstractMusicEntityItem.formatSortKey(entity.getId()));
        saveEntityWithStringSortKey(dynamoDbItem, clazz);
    }

    private <E> void saveEntityWithStringSortKey(E entity, Class<E> clazz) {
        DynamoDbTable<E> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkStringSkTableName, TableSchema.fromBean(clazz)
        );
        catalogTableWithEntitySpecificSchema.putItem(entity);
    }

    private <E> void saveEntityWithNumericSortKey(E entity, Class<E> clazz) {
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

    public List<Artist> listArtists() {
        return listMusicEntities(
                Artist.class,
                ArtistItem.class
        );
    }

    public List<Album> listAlbums() {
        return listMusicEntities(
                Album.class,
                AlbumItem.class
        );
    }

    public List<Track> listTracks() {
        return listMusicEntities(
                Track.class,
                TrackItem.class
        );
    }

    private <E extends BaseEntity, DI extends AbstractMusicEntityItem<E>> List<E> listMusicEntities(
            Class<E> entityClass,
            Class<DI> dynamodbItemClass
    ) {
        DynamoDbTable<DI> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkStringSkTableName, TableSchema.fromBean(dynamodbItemClass)
        );
        String partitionKey = AbstractMusicEntityItem.formatPartitionKey(
                entityClass
        );
        PageIterable<DI> itemPages = catalogTableWithEntitySpecificSchema.query(
                QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(
                                partitionKey
                        ).build()
                )
        );
        return itemPages.stream()
                .map(Page::items)
                .flatMap(Collection::stream)
                .map(AbstractMusicEntityItem::getEntity)
                .collect(Collectors.toList());
    }

    private <E extends BaseEntity, DE extends AbstractMusicEntityItem<E>> E findMusicEntity(
            Class<E> clazz, String id, DE keyHolder, Class<DE> dynamodbItemClass
    ) {
        DynamoDbTable<DE> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkStringSkTableName, TableSchema.fromBean(dynamodbItemClass)
        );
        keyHolder.setPk(AbstractMusicEntityItem.formatPartitionKey(clazz));
        keyHolder.setSk(AbstractMusicEntityItem.formatSortKey(id));
        DE actualItem = catalogTableWithEntitySpecificSchema.getItem(
                keyHolder
        );
        return actualItem == null ? null : actualItem.getEntity();
    }

    public List<Track> listChildTracks(
            Class<? extends BaseEntity> parentEntityClass,
            String parentEntityId
    ) {
        PageIterable<ChildTrackItem> childTracks = iterateChildTracks(
                parentEntityClass, parentEntityId
        );
        return childTracks.stream()
                .map(Page::items)
                .flatMap(Collection::stream)
                .map(ChildTrackItem::getTrackId)
                .map(this::findTrack)
                .collect(Collectors.toList());
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

    public TrackMetadata findTrackMetadata(String path) {
        DynamoDbTable<TrackMetadataItem> catalogTableWithEntitySpecificSchema = dynamodbClient.table(
                stringPkStringSkTableName, TableSchema.fromBean(TrackMetadataItem.class)
        );
        TrackMetadataItem keyHolder = new TrackMetadataItem();
        keyHolder.setPk(TrackMetadataItem.formatPartitionKey());
        keyHolder.setSk(TrackMetadataItem.formatSortKey(path));
        TrackMetadataItem actualItem = catalogTableWithEntitySpecificSchema.getItem(
                keyHolder
        );
        return actualItem == null ? null : actualItem.getData();
    }
}
