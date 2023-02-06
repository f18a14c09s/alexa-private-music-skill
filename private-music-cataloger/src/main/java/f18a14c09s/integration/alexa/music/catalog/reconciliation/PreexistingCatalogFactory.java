package f18a14c09s.integration.alexa.music.catalog.reconciliation;

import f18a14c09s.integration.alexa.music.catalog.DynamoDBCatalogDAO;
import f18a14c09s.integration.alexa.music.entities.*;

import java.util.*;
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
        Map<EntityType, Map<List<String>, String>> entityIdsByTypeAndNaturalKey = new HashMap<>();
        for (Map.Entry<String, List<Artist>> artistNameArtists : preexistingCatalog.getArtistsByName().entrySet()) {
            if (artistNameArtists.getValue().size() > 1) {
                System.out.printf("More than one artist (%s total) with name \"%s\" found.%n",
                        artistNameArtists.getValue()
                                .size(), artistNameArtists.getKey());
                entityIdsByTypeAndNaturalKey.computeIfAbsent(EntityType.ARTIST, key -> new HashMap<>()).put(
                        List.of(artistNameArtists.getKey()), artistNameArtists.getValue().get(0).getId()
                );
            }
        }
        for (Map.Entry<List<String>, List<Album>> albumArtistNameAlbums : preexistingCatalog.getAlbumsByArtistAndName()
                .entrySet()) {
            if (albumArtistNameAlbums.getValue().size() > 1) {
                System.out.printf("More than one album (%s total) with (artist, name) \"%s\" found.%n",
                        albumArtistNameAlbums.getValue()
                                .size(), albumArtistNameAlbums.getKey());
                entityIdsByTypeAndNaturalKey.computeIfAbsent(EntityType.ALBUM, key -> new HashMap<>()).put(
                        albumArtistNameAlbums.getKey(), albumArtistNameAlbums.getValue().get(0).getId()
                );
            }
        }
        for (Map.Entry<List<String>, List<Track>> trackArtistAlbumNameTracks :
                preexistingCatalog.getTracksByArtistAlbumAndName()
                        .entrySet()) {
            if (trackArtistAlbumNameTracks.getValue().size() > 1) {
                System.out.printf("More than one track (%s total) with (artist, album, name) \"%s\" found.%n",
                        trackArtistAlbumNameTracks.getValue()
                                .size(), trackArtistAlbumNameTracks.getKey());
                entityIdsByTypeAndNaturalKey.computeIfAbsent(EntityType.TRACK, key -> new HashMap<>()).put(
                        trackArtistAlbumNameTracks.getKey(), trackArtistAlbumNameTracks.getValue().get(0).getId()
                );
            }
        }
        preexistingCatalog.setEntityIdsByTypeAndNaturalKey(entityIdsByTypeAndNaturalKey);
        //
        return preexistingCatalog;
    }
}
