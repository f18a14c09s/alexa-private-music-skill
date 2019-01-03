package f18a14c09s.integration.alexa.music.data;

import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.EntityTypeName;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "artist_queue_key")
@DiscriminatorValue(EntityTypeName.ARTIST)
public class ArtistAudioQueue extends AbstractAudioQueue {
    @ManyToOne
    @JoinColumn(name = "artist_id", referencedColumnName = "id")
    private Artist artist;
}
