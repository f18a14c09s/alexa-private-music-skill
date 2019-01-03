package f18a14c09s.integration.alexa.music.data;

import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.EntityTypeName;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "album_queue_key")
@DiscriminatorValue(EntityTypeName.ALBUM)
public class AlbumAudioQueue extends AbstractAudioQueue {
    @ManyToOne
    @JoinColumn(name = "album_id", referencedColumnName = "id")
    private Album album;
}
