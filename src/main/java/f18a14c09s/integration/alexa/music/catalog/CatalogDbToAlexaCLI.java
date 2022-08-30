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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CatalogDbToAlexaCLI {
    private static CatalogContentUploadClient catalogContentUploadClient = new CatalogContentUploadClient(
            ""
    );
    private static DynamoDBCatalogDAO catalogDAO = new DynamoDBCatalogDAO(
            "private-music-alexa-skill-StringPartitionKeyStringSortKeyTable",
            "private-music-alexa-skill-StringPartitionKeyNumericSortKeyTable"
    );
    private static String vendorId = "";
    private static String skillId = "";

    public static void main(String... args) throws IOException {
        Map<String, Catalog> skillCatalogsByType = mapSkillCatalogsByType(skillId);
        List<Track> tracks = catalogDAO.listTracks();
        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
        trackCatalog.setEntities(tracks);
        //
        Set<String> uniqueAlbumIds = tracks.stream()
                .map(Track::getAlbums)
                .flatMap(Collection::stream)
                .map(BaseEntityReference::getId)
                .collect(Collectors.toSet());
        List<Album> albums = catalogDAO.listAlbums().stream().filter(
                album -> uniqueAlbumIds.contains(album.getId())
        ).collect(Collectors.toList());
        MusicAlbumCatalog albumCatalog = new MusicAlbumCatalog();
        albumCatalog.setEntities(albums);
        //
        Set<String> uniqueArtistIds = tracks.stream()
                .map(Track::getArtists)
                .flatMap(Collection::stream)
                .map(BaseEntityReference::getId)
                .collect(Collectors.toSet());
        List<Artist> artists = catalogDAO.listArtists().stream().filter(
                artist -> uniqueArtistIds.contains(artist.getId())
        ).collect(Collectors.toList());
        MusicGroupCatalog artistCatalog = new MusicGroupCatalog();
        artistCatalog.setEntities(artists);
        //
        for (AbstractCatalog catalog : List.of(
                trackCatalog,
                albumCatalog,
                artistCatalog
        )) {
            catalog.setLocales(List.of(Locale.en_US()));
            catalogContentUploadClient.uploadCatalogContent(
                    skillCatalogsByType.get(catalog.getType()).getId(),
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
