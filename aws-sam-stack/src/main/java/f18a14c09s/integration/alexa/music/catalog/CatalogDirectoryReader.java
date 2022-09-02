package f18a14c09s.integration.alexa.music.catalog;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import f18a14c09s.integration.alexa.music.catalog.data.*;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.EntityType;
import f18a14c09s.integration.alexa.music.entities.Track;

public class CatalogDirectoryReader {
    private static <C extends AbstractCatalog> C readCatalog(File catalogFile, Class<C> catalogClass) {
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            return jsonMapper.readValue(catalogFile, catalogClass);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> extractMusicEntityKey(BaseEntity musicEntity) {
        List<String> musicEntityKey = new ArrayList<>();
        if (musicEntity instanceof Album) {
            Album album = (Album) musicEntity;
            musicEntityKey.add(
                    album.getArtists().get(0).getNames().get(0).getValue());
        }
        if (musicEntity instanceof Track) {
            Track track = (Track) musicEntity;
            musicEntityKey.add(
                    track.getArtists().get(0).getNames().get(0).getValue());
            musicEntityKey.add(
                    track.getAlbums().get(0).getNames().get(0).getValue());
        }
        musicEntityKey.add(musicEntity.getNames().get(0).getValue());
        return musicEntityKey;
    }

    public static Map<EntityType, Map<List<String>, Set<String>>> readCatalogDirectory(
            Map<EntityType, File> catalogFiles
    ) {
        Map<EntityType, Class<? extends AbstractCatalog>> catalogClasses = Map.of(
                EntityType.ARTIST,
                MusicGroupCatalog.class,
                EntityType.ALBUM,
                MusicGroupCatalog.class,
                EntityType.TRACK,
                MusicRecordingCatalog.class
        );
        Map<EntityType, Map<List<String>, Set<String>>> entityIdsByTypeAndNaturalKey = new HashMap<>();
        for (EntityType entityType : catalogFiles.keySet()) {
            entityIdsByTypeAndNaturalKey.put(entityType, new HashMap<>());
            AbstractCatalog catalog = readCatalog(
                catalogFiles.get(entityType),
                    catalogClasses.get(entityType));
            for (BaseEntity musicEntity : catalog.getEntities()) {
                entityIdsByTypeAndNaturalKey.get(entityType).computeIfAbsent(
                        extractMusicEntityKey(musicEntity),
                        key -> new HashSet<>()).add(
                                musicEntity.getId());
            }
        }
        return entityIdsByTypeAndNaturalKey;
    }
}
