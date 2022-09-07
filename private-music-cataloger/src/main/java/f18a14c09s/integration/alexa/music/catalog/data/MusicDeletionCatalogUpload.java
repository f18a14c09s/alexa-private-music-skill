package f18a14c09s.integration.alexa.music.catalog.data;

import f18a14c09s.integration.alexa.data.Locale;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MusicDeletionCatalogUpload {
    private String type;
    private Double version;
    private List<Locale> locales;
    private List<MusicEntityDeletion> entities;
}
