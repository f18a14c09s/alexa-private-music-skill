package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.music.entities.Artist;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;


@Getter
@Setter
@DynamoDbBean
public class ArtistItem extends AbstractMusicEntityItem<Artist> {
    private Artist entity;
}