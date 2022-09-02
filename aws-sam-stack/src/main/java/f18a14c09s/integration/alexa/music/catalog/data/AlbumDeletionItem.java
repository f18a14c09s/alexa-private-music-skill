package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.music.entities.Album;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Getter
@Setter
@DynamoDbBean
public class AlbumDeletionItem extends AbstractDeletionItem<Album> {
    private Album entity;
}
