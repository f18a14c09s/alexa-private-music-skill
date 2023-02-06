package f18a14c09s.integration.alexa.music.catalog.reconciliation;

import f18a14c09s.integration.alexa.music.catalog.AlbumKey;
import f18a14c09s.integration.alexa.music.entities.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class PreexistingCatalog {
    private Map<String, Artist> artistsById;
    private Map<String, Album> albumsById;
    private Map<String, Track> tracksById;
    private Map<String, List<Artist>> artistsByName;
    private Map<List<String>, List<Album>> albumsByArtistAndName;
    private Map<List<String>, List<Track>> tracksByArtistAlbumAndName;
    private Map<EntityType, Map<List<String>, String>> entityIdsByTypeAndNaturalKey;

    public Artist findArtistById(String artistId) {
        return artistsById.get(artistId);
    }

    public Album findAlbumById(String albumId) {
        return albumsById.get(albumId);
    }

    public Track findTrackById(String trackId) {
        return tracksById.get(trackId);
    }

    public Collection<Artist> matchArtistsByName(String artistName) {
        return artistsByName.get(artistName);
    }

    public Collection<Album> matchAlbumsByNaturalKey(AlbumKey albumKey) {
        return albumsByArtistAndName.get(albumKey.toList());
    }

    public Collection<Track> matchTracksByArtistAlbumAndName(Track track) {
        List<List<String>>
                artistsAlbumsAndNames =
                track.getNames()
                        .stream()
                        .map(EntityName::getValue)
                        .flatMap(trackName -> track.getArtists()
                                .stream()
                                .flatMap(artistReference -> artistReference.getNames().stream())
                                .map(EntityName::getValue)
                                .flatMap(artistName -> track.getAlbums()
                                        .stream()
                                        .flatMap(albumReference -> albumReference.getNames()
                                                .stream()
                                                .map(EntityName::getValue)
                                                .map(albumName -> List.of(artistName, albumName, trackName)))))
                        .collect(Collectors.toList());
        return artistsAlbumsAndNames
                .stream()
                .flatMap(artistNameAlbumName -> tracksByArtistAlbumAndName.computeIfAbsent(artistNameAlbumName,
                                key -> List.of())
                        .stream())
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity(), (lhs, rhs) -> lhs))
                .values();
    }
}
