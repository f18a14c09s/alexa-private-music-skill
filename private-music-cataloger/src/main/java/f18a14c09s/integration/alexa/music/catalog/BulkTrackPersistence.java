package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.EntityName;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.json.JSONAdapter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BulkTrackPersistence {
    private List<Future<Track>> tasks = new ArrayList<>();
    private ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(20);
    private DynamoDBCatalogDAO catalogDAO;
    private JSONAdapter jsonAdapter = new JSONAdapter();

    public BulkTrackPersistence(DynamoDBCatalogDAO catalogDAO) {
        this.catalogDAO = catalogDAO;
    }

    public void saveTracks(List<Track> tracks) throws InterruptedException {
        tasks.addAll(
                threadPoolExecutor.invokeAll(
                        tracks.stream().<Callable<Track>>map(
                                track -> () -> {
                                    try {
                                        catalogDAO.save(track);
                                    } catch (RuntimeException e) {
                                        System.out.printf(
                                                "Failure:%n\t%s%n",
                                                jsonAdapter.writeValueAsString(track)
                                        );
                                        throw e;
                                    }
                                    return track;
                                }
                        ).collect(Collectors.toList())
                )
        );
    }

    public void associateTracksToArtists(Map<String, List<Track>> childTracksByArtistId) throws InterruptedException {
        tasks.addAll(
                threadPoolExecutor.invokeAll(
                        childTracksByArtistId.entrySet().stream().<Callable<Track>>map(
                                artistIdChildTracks -> () -> {
                                    catalogDAO.saveChildTrackAssociations(
                                            Artist.class,
                                            artistIdChildTracks.getKey(),
                                            artistIdChildTracks.getValue().stream().sorted(
                                                    Comparator.<Track, Long>comparing(
                                                            track -> track.getAlbums()
                                                                    .stream()
                                                                    .map(
                                                                            albumReference -> Optional.ofNullable(albumReference.getNaturalOrder())
                                                                                    .orElse(0L)
                                                                    )
                                                                    .findAny()
                                                                    .orElse(0L)
                                                    ).thenComparing(
                                                            track -> track.getAlbums().stream().flatMap(
                                                                    albumReference -> albumReference.getNames()
                                                                            .stream()
                                                            ).map(EntityName::getValue).findAny().orElse("")
                                                    ).thenComparing(
                                                            track -> Optional.ofNullable(track.getNaturalOrder())
                                                                    .orElse(0L)
                                                    )
                                            ).collect(Collectors.toList())
                                    );
                                    return null;
                                }
                        ).collect(Collectors.toList())
                )
        );
    }

    public void associateTracksToAlbums(Map<String, List<Track>> childTracksByAlbumId) throws InterruptedException {
        tasks.addAll(
                threadPoolExecutor.invokeAll(
                        childTracksByAlbumId.entrySet().stream().<Callable<Track>>map(
                                albumIdChildTracks -> () -> {
                                    catalogDAO.saveChildTrackAssociations(
                                            Album.class,
                                            albumIdChildTracks.getKey(),
                                            albumIdChildTracks.getValue().stream().sorted(
                                                    Comparator.comparing(
                                                            track -> Optional.ofNullable(track.getNaturalOrder())
                                                                    .orElse(0L)
                                                    )
                                            ).collect(Collectors.toList())
                                    );
                                    return null;
                                }
                        ).collect(Collectors.toList())
                )
        );
    }

    public void saveTracksToCatalog(List<Track> tracks, Map<String, List<Track>> childTracksByArtistId, Map<String,
            List<Track>> childTracksByAlbumId) throws IOException {
        try {
            saveTracks(tracks);
            associateTracksToArtists(childTracksByArtistId);
            associateTracksToAlbums(childTracksByAlbumId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPoolExecutor.shutdown();
        }

        for (Future<Track> task : tasks) {
            try {
                Track success = task.get();
                System.out.printf(
                        "Success:%n\t%s%n",
                        jsonAdapter.writeValueAsString(success)
                );
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
