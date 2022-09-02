package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.music.entities.EntityType;
import f18a14c09s.integration.mp3.TrackMetadata;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@DynamoDbBean
public class TrackMetadataItem implements StringPkStringSkDynamoDbItem {
    private String pk;
    private String sk;
    private TrackMetadata data;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public static String formatPartitionKey() {
        return "LISTTYPE=MUSICMETADATA";
    }

    public static String formatSortKey(
            String path
    ) {
        return String.format(
                "METADATATYPE=%s,ID=%s",
                EntityType.TRACK,
                path
        );
    }
}
