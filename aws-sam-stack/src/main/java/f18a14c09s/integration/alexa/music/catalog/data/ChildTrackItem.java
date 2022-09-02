package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@Getter
@Setter
@DynamoDbBean
public class ChildTrackItem implements StringPkIntegerSkDynamoDbItem {
    private String pk;
    private Integer sk;
    private String trackId;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public Integer getSk() {
        return sk;
    }

    public static String formatPartitionKey(
            Class<? extends BaseEntity> parentEntityClass,
            String parentEntityId
    ) {
        return String.format(
                "MUSICENTITYTYPE=%s,ENTITYID=%s,LISTTYPE=CHILDTRACKS",
                parentEntityClass.getSimpleName(),
                parentEntityId
        );
    }
}