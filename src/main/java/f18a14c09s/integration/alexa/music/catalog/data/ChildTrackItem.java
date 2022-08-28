package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ChildTrackItem implements StringPkIntegerSkDynamoDbItem {
    private String pk;
    private Integer sk;
    private String trackId;

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