package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Setter
public abstract class AbstractMusicEntityItem<E extends BaseEntity> implements StringPkStringSkDynamoDbItem {
    private String pk;
    private String sk;

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

    public static String formatPartitionKey(
            Class<? extends BaseEntity> clazz
    ) {
        return String.format(
                "MUSICENTITYTYPE=%s",
                clazz.getSimpleName()
        );
    }

    public static String formatSortKey(
            String id
    ) {
        return String.format(
                "ENTITYID=%s,LISTTYPE=MUSICENTITIES",
                id
        );
    }
}