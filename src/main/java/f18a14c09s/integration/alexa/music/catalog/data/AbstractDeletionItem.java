package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class AbstractDeletionItem<E extends BaseEntity> implements StringPkStringSkDynamoDbItem {
    public enum DeletionSource {
        OUTDATED_CATALOG_FILE,
        ENTITY_NOT_FOUND_SKILL_RESPONSE
    }

    private String pk;
    private String sk;
    private DeletionSource deletionSource;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public abstract E getEntity();

    public abstract void setEntity(E entity);

    public static String formatPartitionKey() {
        return "LISTTYPE=MUSICCATALOGENTITYDELETION";
    }

    private static String formatSortKey(
            Class<? extends BaseEntity> musicEntityClass,
            String entityId
    ) {
        List<String> sortKeyComponents = new ArrayList<>();
        sortKeyComponents.add(String.format(
                "MUSICENTITYTYPE=%s",
                musicEntityClass.getSimpleName()
        ));
        if (entityId != null) {
            sortKeyComponents.add(String.format(
                    "ENTITYID=%s",
                    entityId
            ));
        }
        return String.join(",", sortKeyComponents);
    }

    public static String formatSortKey(
            Class<? extends BaseEntity> musicEntityClass
    ) {
        return formatSortKey(musicEntityClass, null);
    }

    public static String formatSortKey(
            BaseEntity entity
    ) {
        return formatSortKey(entity.getClass(), entity.getId());
    }
}
