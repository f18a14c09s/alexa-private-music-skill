package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicAlbumCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicGroupCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicRecordingCatalog;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.BaseEntityReference;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.alexa.smapi.CatalogContentUploadClient;
import f18a14c09s.integration.alexa.smapi.data.Catalog;
import f18a14c09s.integration.alexa.smapi.data.Upload;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CatalogDbToAlexaCLI {
    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static CatalogContentUploadClient catalogContentUploadClient = new CatalogContentUploadClient(
            System.getenv("ALEXA_SMAPI_ACCESS_TOKEN")
    );
    private static DynamoDBCatalogDAO catalogDAO = new DynamoDBCatalogDAO(
            "private-music-alexa-skill-StringPartitionKeyStringSortKeyTable",
            "private-music-alexa-skill-StringPartitionKeyNumericSortKeyTable"
    );
    private static String vendorId = System.getenv("ALEXA_SKILL_VENDOR_ID");
    private static String skillId = System.getenv("PRIVATE_MUSIC_ALEXA_SKILL_ID");

    public static void main(String... args) throws IOException {
        Map<String, Catalog> skillCatalogsByType = mapSkillCatalogsByType(skillId);
        LOGGER.info(String.format(
                "Current catalogs for skill ID %s:%n%s",
                skillId,
                JSON_MAPPER.writeValueAsString(skillCatalogsByType)
        ));
        List<Track> tracks = catalogDAO.listTracks();
        LOGGER.info(String.format("%s tracks found.", tracks.size()));
        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
        trackCatalog.setEntities(tracks);
        //
        Set<String> uniqueAlbumIds = tracks.stream()
                .map(Track::getAlbums)
                .flatMap(Collection::stream)
                .map(BaseEntityReference::getId)
                .collect(Collectors.toSet());
        List<Album> albums = catalogDAO.listAlbums();
        int totalAlbums = albums.size();
        albums = albums.stream().filter(
                album -> uniqueAlbumIds.contains(album.getId())
        ).collect(Collectors.toList());
        LOGGER.info(String.format(
                "%s albums found--filtered down from %s.",
                albums.size(),
                totalAlbums
        ));
        MusicAlbumCatalog albumCatalog = new MusicAlbumCatalog();
        albumCatalog.setEntities(albums);
        //
        Set<String> uniqueArtistIds = tracks.stream()
                .map(Track::getArtists)
                .flatMap(Collection::stream)
                .map(BaseEntityReference::getId)
                .collect(Collectors.toSet());
        List<Artist> artists = catalogDAO.listArtists();
        int totalArtists = artists.size();
        artists = artists.stream().filter(
                artist -> uniqueArtistIds.contains(artist.getId())
        ).collect(Collectors.toList());
        LOGGER.info(String.format(
                "%s artists found--filtered down from %s.",
                artists.size(),
                totalArtists
        ));
        MusicGroupCatalog artistCatalog = new MusicGroupCatalog();
        artistCatalog.setEntities(artists);
        
        for (AbstractCatalog catalog : List.of(
                trackCatalog,
                albumCatalog,
                artistCatalog
        )) {
            catalog.setLocales(List.of(Locale.en_US()));
            Catalog targetCatalog = skillCatalogsByType.get(catalog.getType());
            List<Upload> pastUploads = catalogContentUploadClient.listUploads(targetCatalog.getId());
            pastUploads.sort(
                Comparator.comparing(Upload::getCreatedDate)
            );
            pastUploads = pastUploads.stream().map(
                upload -> {
                        try {
                                return catalogContentUploadClient.getUpload(
                                        upload.getCatalogId(),
                                        upload.getId()
                                );
                        } catch(IOException e) {
                                throw new RuntimeException(e);
                        }
                }
            ).collect(Collectors.toList());

            LOGGER.info(String.format(
                "Past uploads for %s catalog:%n%s",
                catalog.getType(),
                JSON_MAPPER.writeValueAsString(pastUploads)
            ));

            catalogContentUploadClient.uploadCatalogContent(
                    targetCatalog.getId(),
                    catalog
            );
        }
    }

    private static Map<String, Catalog> mapSkillCatalogsByType(
            String skillId
    ) throws IOException {
        Map<String, Catalog> skillCatalogsByType = new HashMap<>();
        for (Catalog catalog : catalogContentUploadClient.listCatalogs(vendorId)) {
            if (Optional.ofNullable(catalog.getAssociatedSkillIds()).orElse(List.of()).contains(skillId)) {
                skillCatalogsByType.put(
                        catalog.getType(),
                        catalog
                );
            }

        }
        return skillCatalogsByType;
    }
}
