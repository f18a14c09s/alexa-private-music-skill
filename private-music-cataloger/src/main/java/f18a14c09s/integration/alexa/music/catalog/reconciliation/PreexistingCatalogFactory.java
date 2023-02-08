package f18a14c09s.integration.alexa.music.catalog.reconciliation;

import f18a14c09s.integration.alexa.music.catalog.DynamoDBCatalogDAO;
import f18a14c09s.integration.alexa.music.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PreexistingCatalogFactory {
    public static PreexistingCatalog loadPreexistingCatalog(DynamoDBCatalogDAO catalogDAO) {
        PreexistingCatalog preexistingCatalog = new PreexistingCatalog();
        //
        List<Artist>
                artists =
                catalogDAO.listArtists()
                        .stream()
                        .filter(artist -> artist.getDeleted() == null || !artist.getDeleted())
                        .collect(Collectors.toList());
        List<Album>
                albums =
                catalogDAO.listAlbums()
                        .stream()
                        .filter(album -> album.getDeleted() == null || !album.getDeleted())
                        .collect(Collectors.toList());
        List<Track>
                tracks =
                catalogDAO.listTracks()
                        .stream()
                        .filter(track -> track.getDeleted() == null || !track.getDeleted())
                        .collect(Collectors.toList());
        //
        preexistingCatalog.setArtistsById(artists.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity())));
        preexistingCatalog.setArtistsByName(new HashMap<>());
        for (Artist artist : artists) {
            for (EntityName name : Optional.ofNullable(artist.getNames()).orElse(List.of())) {
                preexistingCatalog.getArtistsByName().computeIfAbsent(
                        name.getValue(), key -> new ArrayList<>()
                ).add(artist);
            }
        }
        //
        preexistingCatalog.setAlbumsById(albums.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity())));
        preexistingCatalog.setAlbumsByArtistAndName(new HashMap<>());
        for (Album album : albums) {
            for (EntityName albumName : Optional.ofNullable(album.getNames()).orElse(List.of())) {
                for (ArtistReference artistReference : Optional.ofNullable(album.getArtists()).orElse(List.of())) {
                    for (EntityName artistName : Optional.ofNullable(artistReference.getNames()).orElse(List.of())) {
                        List<String> artistAndName = List.of(artistName.getValue(), albumName.getValue());
                        preexistingCatalog.getAlbumsByArtistAndName().computeIfAbsent(
                                artistAndName, key -> new ArrayList<>()
                        ).add(album);
                    }
                }
            }
        }
        //
        preexistingCatalog.setTracksById(tracks.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity())));
        preexistingCatalog.setTracksByArtistAlbumAndName(new HashMap<>());
        for (Track track : tracks) {
            for (EntityName trackName : Optional.ofNullable(track.getNames()).orElse(List.of())) {
                for (AlbumReference albumReference : track.getAlbums()) {
                    for (EntityName albumName : Optional.ofNullable(albumReference.getNames()).orElse(List.of())) {
                        for (ArtistReference artistReference : Optional.ofNullable(track.getArtists())
                                .orElse(List.of())) {
                            for (EntityName artistName : Optional.ofNullable(artistReference.getNames())
                                    .orElse(List.of())) {
                                List<String>
                                        artistAlbumAndName =
                                        List.of(artistName.getValue(), albumName.getValue(), trackName.getValue());
                                preexistingCatalog.getTracksByArtistAlbumAndName().computeIfAbsent(
                                        artistAlbumAndName, key -> new ArrayList<>()
                                ).add(track);
                            }
                        }
                    }
                }
            }
        }
        //
        return preexistingCatalog;
    }
}
