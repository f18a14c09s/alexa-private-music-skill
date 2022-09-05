package f18a14c09s.integration.alexa.music.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import f18a14c09s.integration.alexa.music.entities.*;

import java.util.*;
import java.util.stream.Collectors;

public class CrossReferenceOldEntityIdsCLI {
    private static final DynamoDBCatalogDAO CATALOG_DAO = new DynamoDBCatalogDAO(
            "private-music-alexa-skill-StringPartitionKeyStringSortKeyTable",
            "private-music-alexa-skill-StringPartitionKeyNumericSortKeyTable"
    );
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static final class CrossReferenceOldTrackIds {
        private List<Track> oldTracks = CATALOG_DAO.listTrackDeletions();
        private List<Track> newTracks = CATALOG_DAO.listTracks();

        List<String> extractKey(Track track) {
            return List.of(
                    Optional.ofNullable(track.getArtists())
                            .orElse(List.of())
                            .stream()
                            .map(BaseEntityReference::getNames)
                            .flatMap(Collection::stream)
                            .filter(Objects::nonNull)
                            .map(EntityName::getValue)
                            .filter(Objects::nonNull)
                            .findAny()
                            .orElse("Unknown").toLowerCase(),
                    Optional.ofNullable(track.getAlbums())
                            .orElse(List.of())
                            .stream()
                            .map(BaseEntityReference::getNames)
                            .flatMap(Collection::stream)
                            .filter(Objects::nonNull)
                            .map(EntityName::getValue)
                            .filter(Objects::nonNull)
                            .findAny()
                            .orElse("Unknown").toLowerCase().replaceAll(" *\\[explicit\\]", ""),
                    Optional.ofNullable(track.getNames())
                            .orElse(List.of())
                            .stream()
                            .filter(Objects::nonNull)
                            .map(EntityName::getValue)
                            .filter(Objects::nonNull)
                            .findAny()
                            .orElse("Unknown").toLowerCase().replaceAll(" *\\[explicit\\]", "")
            );
        }

        void crossReferenceOldIds() {
            Map<List<String>, List<Track>> newTracksByNaturalKey = newTracks.stream().collect(
                    Collectors.groupingBy(
                            this::extractKey
                    )
            );
            int i = 1;
            for (Track oldTrack : oldTracks) {
                List<Track> matchingNewTracks = newTracksByNaturalKey.get(
                        extractKey(oldTrack)
                );
                if (matchingNewTracks == null) {
                    System.out.printf(
                            "%s.  %s%n",
                            i++,
                            extractKey(oldTrack)
                    );
                    continue;
                }
                String newUrl = matchingNewTracks.get(0).getUrl();
                oldTrack.setUrl(newUrl);
                oldTrack.setDeleted(true);
                CATALOG_DAO.save(
                        oldTrack
                );
            }
            Map<String, List<Track>> oldTracksByOldArtistId = oldTracks.stream().collect(
                    Collectors.groupingBy(
                            track -> track.getArtists().get(0).getId()
                    )
            );
            Map<String, List<Track>> oldTracksByOldAlbumId = oldTracks.stream().collect(
                    Collectors.groupingBy(
                            track -> track.getAlbums().get(0).getId()
                    )
            );
            for (String artistId : oldTracksByOldArtistId.keySet()) {
                List<Track> artistTracks = oldTracksByOldArtistId.get(artistId);
                artistTracks.sort(
                        Comparator.comparing(
                                (Track track) -> Optional.ofNullable(track.getAlbums())
                                        .orElse(List.of())
                                        .stream()
                                        .map(AlbumReference::getNaturalOrder)
                                        .filter(Objects::nonNull)
                                        .findAny()
                                        .orElse(0L)
                        ).thenComparing(
                                track -> Optional.ofNullable(track.getAlbums())
                                        .orElse(List.of())
                                        .stream()
                                        .map(AlbumReference::getNames)
                                        .flatMap(Collection::stream)
                                        .map(EntityName::getValue)
                                        .filter(Objects::nonNull)
                                        .findAny()
                                        .orElse("")
                        ).thenComparing(
                                track -> Optional.ofNullable(track.getNaturalOrder()).orElse(0L)
                        ).thenComparing(
                                track -> Optional.ofNullable(track.getNames())
                                        .orElse(List.of())
                                        .stream()
                                        .map(EntityName::getValue)
                                        .filter(Objects::nonNull)
                                        .findAny()
                                        .orElse("")
                        ).thenComparing(
                                track -> Optional.ofNullable(track.getUrl()).orElse("")
                        )
                );
                CATALOG_DAO.saveChildTrackAssociations(
                        Artist.class,
                        artistId,
                        artistTracks
                );
            }
            for (String albumId : oldTracksByOldAlbumId.keySet()) {
                List<Track> albumTracks = oldTracksByOldAlbumId.get(albumId);
                albumTracks.sort(
                        Comparator.comparing(
                                (Track track) -> Optional.ofNullable(track.getNaturalOrder()).orElse(0L)
                        ).thenComparing(
                                track -> Optional.ofNullable(track.getNames())
                                        .orElse(List.of())
                                        .stream()
                                        .map(EntityName::getValue)
                                        .filter(Objects::nonNull)
                                        .findAny()
                                        .orElse("")
                        ).thenComparing(
                                track -> Optional.ofNullable(track.getUrl()).orElse("")
                        )
                );
                CATALOG_DAO.saveChildTrackAssociations(
                        Album.class,
                        albumId,
                        albumTracks
                );
            }
        }
    }

    public static final class CrossReferenceOldAlbumIds {
        private List<Album> oldAlbums = CATALOG_DAO.listAlbumDeletions();
        private List<Album> newAlbums = CATALOG_DAO.listAlbums();

        List<String> extractKey(Album album) {
            return List.of(
                    Optional.ofNullable(album.getArtists())
                            .orElse(List.of())
                            .stream()
                            .map(BaseEntityReference::getNames)
                            .flatMap(Collection::stream)
                            .filter(Objects::nonNull)
                            .map(EntityName::getValue)
                            .filter(Objects::nonNull)
                            .findAny()
                            .orElse("Unknown").toLowerCase(),
                    Optional.ofNullable(album.getNames())
                            .orElse(List.of())
                            .stream()
                            .filter(Objects::nonNull)
                            .map(EntityName::getValue)
                            .filter(Objects::nonNull)
                            .findAny()
                            .orElse("Unknown").toLowerCase().replaceAll(" *\\[explicit\\]", "")
            );
        }

        void crossReferenceOldIds() {
            Map<List<String>, List<Album>> newAlbumsByNaturalKey = newAlbums.stream().collect(
                    Collectors.groupingBy(
                            this::extractKey
                    )
            );
            int i = 1;
            for (Album oldAlbum : oldAlbums) {
                List<Album> matchingNewAlbums = newAlbumsByNaturalKey.get(
                        extractKey(oldAlbum)
                );
                if (matchingNewAlbums == null) {
                    System.out.printf(
                            "%s.  %s%n",
                            i++,
                            extractKey(oldAlbum)
                    );
                    continue;
                }
                oldAlbum.setDeleted(true);
                CATALOG_DAO.save(
                        oldAlbum
                );
            }
        }
    }

    public static final class CrossReferenceOldArtistIds {
        private List<Artist> oldArtists = CATALOG_DAO.listArtistDeletions();
        private List<Artist> newArtists = CATALOG_DAO.listArtists();

        String extractKey(Artist artist) {
            return Optional.ofNullable(artist.getNames())
                            .orElse(List.of())
                            .stream()
                            .filter(Objects::nonNull)
                            .map(EntityName::getValue)
                            .filter(Objects::nonNull)
                            .findAny()
                            .orElse("Unknown").toLowerCase().replaceAll(" *\\[explicit\\]", "");
        }

        void crossReferenceOldIds() {
            Map<String, List<Artist>> newArtistsByNaturalKey = newArtists.stream().collect(
                    Collectors.groupingBy(
                            this::extractKey
                    )
            );
            int i = 1;
            for (Artist oldArtist : oldArtists) {
                List<Artist> matchingNewArtists = newArtistsByNaturalKey.get(
                        extractKey(oldArtist)
                );
                if (matchingNewArtists == null) {
                    System.out.printf(
                            "%s.  %s%n",
                            i++,
                            extractKey(oldArtist)
                    );
                    continue;
                }
                oldArtist.setDeleted(true);
                CATALOG_DAO.save(
                        oldArtist
                );
            }
        }
    }

    public static void main(String... args) {
        new CrossReferenceOldTrackIds().crossReferenceOldIds();
        new CrossReferenceOldAlbumIds().crossReferenceOldIds();
        new CrossReferenceOldArtistIds().crossReferenceOldIds();
    }
}
