package f18a14c09s.integration.alexa.music.data;

import f18a14c09s.integration.alexa.music.entities.Track;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@Table(name = "audio_queue")
@DiscriminatorColumn(name = "type")
public abstract class AbstractAudioQueue {
    @Id
    @GeneratedValue
    private Long id;

    @OneToMany
    @JoinTable(name = "audio_queue_songs")
    @OrderColumn(name = "queue_order")
    private List<Track> tracks;
}
