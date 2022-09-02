package f18a14c09s.integration.alexa.music.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicAlbumCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicGroupCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicRecordingCatalog;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.alexa.smapi.CatalogContentUploadClient;
import f18a14c09s.integration.alexa.smapi.data.Catalog;
import f18a14c09s.integration.alexa.smapi.data.Upload;
import f18a14c09s.util.StringObjectMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static boolean uploadLatestCatalogs = false;

    private static <E> List<E> concat(
            Collection<E> c1,
            Collection<E> c2
    ) {
        return Stream.concat(
                c1.stream(),
                c2.stream()
        ).collect(Collectors.toList());
    }

    private static <E extends BaseEntity> List<E> filterDeletedEntityAttributes(
            Collection<E> entities,
            Class<E> clazz
    ) {
        return entities.stream().map(
                deletion -> filterDeletedEntityAttributes(
                        deletion,
                        clazz
                )
        ).collect(Collectors.toList());
    }

    private static <E extends BaseEntity> E filterDeletedEntityAttributes(
            E entity,
            Class<E> clazz
    ) {
        try {
            entity = JSON_MAPPER.readValue(
                    JSON_MAPPER.writeValueAsString(
                            JSON_MAPPER.readValue(
                                    JSON_MAPPER.writeValueAsString(
                                            entity
                                    ),
                                    StringObjectMap.class
                            ).entrySet().stream().filter(
                                    mapEntry -> List.of(
                                            "id",
                                            "lastUpdatedTime"
                                    ).contains(mapEntry.getKey())
                            ).collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue
                            ))
                    ),
                    clazz
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        entity.setDeleted(true);
        return entity;
    }

    public static void main(String... args) throws IOException, InterruptedException {
        Map<String, Catalog> skillCatalogsByType = mapSkillCatalogsByType(skillId);
        LOGGER.info(String.format(
                "Current catalogs for skill ID %s:%n%s",
                skillId,
                JSON_MAPPER.writeValueAsString(skillCatalogsByType)
        ));

        Map<String, String> uploadIdsByCatalogId = new HashMap<>();

        if (uploadLatestCatalogs) {
            List<Track> tracks = catalogDAO.listTracks();
            List<Track> trackDeletions = catalogDAO.listTrackDeletions();
            LOGGER.info(String.format("%s tracks, %s track deletions found.", tracks.size(), trackDeletions.size()));
            MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
            trackDeletions = filterDeletedEntityAttributes(
                    trackDeletions,
                    Track.class
            );
            trackCatalog.setEntities(concat(
                    tracks,
                    trackDeletions
            ));
            trackCatalog.setLocales(List.of(Locale.en_US()));
            //
            Set<String> uniqueAlbumIds = tracks.stream()
                    .map(Track::getAlbums)
                    .flatMap(Collection::stream)
                    .map(BaseEntityReference::getId)
                    .collect(Collectors.toSet());
            List<Album> albums = catalogDAO.listAlbums();
            List<Album> albumDeletions = catalogDAO.listAlbumDeletions();
            int totalAlbums = albums.size();
            albums = albums.stream().filter(
                    album -> uniqueAlbumIds.contains(album.getId())
            ).collect(Collectors.toList());
            LOGGER.info(String.format(
                    "%s albums found--filtered down from %s; %s album deletions found.",
                    albums.size(),
                    totalAlbums,
                    albumDeletions.size()
            ));
            MusicAlbumCatalog albumCatalog = new MusicAlbumCatalog();
            albumDeletions = filterDeletedEntityAttributes(
                    albumDeletions,
                    Album.class
            );
            albumCatalog.setEntities(concat(
                    albums,
                    albumDeletions
            ));
            albumCatalog.setLocales(List.of(Locale.en_US()));
            //
            Set<String> uniqueArtistIds = tracks.stream()
                    .map(Track::getArtists)
                    .flatMap(Collection::stream)
                    .map(BaseEntityReference::getId)
                    .collect(Collectors.toSet());
            List<Artist> artists = catalogDAO.listArtists();
            List<Artist> artistDeletions = catalogDAO.listArtistDeletions();
            int totalArtists = artists.size();
            artists = artists.stream().filter(
                    artist -> uniqueArtistIds.contains(artist.getId())
            ).collect(Collectors.toList());
            LOGGER.info(String.format(
                    "%s artists found--filtered down from %s; %s artist deletions found.",
                    artists.size(),
                    totalArtists,
                    artistDeletions.size()
            ));
            MusicGroupCatalog artistCatalog = new MusicGroupCatalog();
            artistDeletions = filterDeletedEntityAttributes(
                    artistDeletions,
                    Artist.class
            );
            artistCatalog.setEntities(concat(
                    artists,
                    artistDeletions
            ));
            artistCatalog.setLocales(List.of(Locale.en_US()));

            for (AbstractCatalog catalog : List.of(
                    trackCatalog,
                    albumCatalog,
                    artistCatalog
            )) {
                Catalog targetCatalog = skillCatalogsByType.get(catalog.getType());

                String uploadId = catalogContentUploadClient.uploadCatalogContent(
                        targetCatalog.getId(),
                        catalog
                );
                uploadIdsByCatalogId.put(
                        targetCatalog.getId(),
                        uploadId
                );
            }
        } else {
            for (String catalogType : skillCatalogsByType.keySet()) {
                Catalog catalog = skillCatalogsByType.get(catalogType);
                Upload mostRecentUpload = catalogContentUploadClient.listUploads(catalog.getId())
                        .stream()
                        .max(Comparator.comparing(Upload::getCreatedDate))
                        .orElse(null);
                if (mostRecentUpload == null) {
                    LOGGER.info(String.format(
                            "No uploads found for %s catalog (ID=%s).",
                            catalog.getType(),
                            catalog.getId()
                    ));
                    continue;
                }
                uploadIdsByCatalogId.put(catalog.getId(), mostRecentUpload.getId());
            }
        }

        for (String catalogType : skillCatalogsByType.keySet()) {
            Catalog targetCatalog = skillCatalogsByType.get(catalogType);
            checkIngestionOfCatalogUpload(
                    targetCatalog,
                    uploadIdsByCatalogId.get(targetCatalog.getId())
            );
        }
    }

    public static void checkIngestionOfCatalogUpload(
            Catalog catalog,
            String uploadId
    ) throws IOException, InterruptedException {
        for (; ; ) {
            Upload upload = catalogContentUploadClient.getUpload(
                    catalog.getId(),
                    uploadId
            );

            if (upload.getStatus().equals("SUCCEEDED")) {
                LOGGER.info(String.format(
                        "%s catalog successfully uploaded and ingested.",
                        catalog.getType()
                ));
                return;
            } else if (upload.getStatus().equals("IN_PROGRESS")) {
                LOGGER.info(String.format(
                        ("%s catalog (ID=%s) has upload (ID=%s) status: %s."
                                + "%n\tCheck back later as the process may take some time."
                                + "%n\tIngestion status: %s%n"),
                        catalog.getType(),
                        catalog.getId(),
                        upload.getId(),
                        upload.getStatus(),
                        JSON_MAPPER.writeValueAsString(
                                Optional.ofNullable(upload.getIngestionSteps())
                                        .orElse(List.of())
                                        .stream()
                                        .collect(Collectors.toMap(
                                                Upload.IngestionStep::getName,
                                                Upload.IngestionStep::getStatus
                                        ))
                        )
                ));
                return;
            }

            LOGGER.warning(String.format(
                    "%s catalog (ID=%s) has upload (ID=%s) status: %s.%n",
                    catalog.getType(),
                    catalog.getId(),
                    upload.getId(),
                    upload.getStatus()
            ));

            List<Upload.IngestionStep> failedIngestionSteps = Optional.ofNullable(upload.getIngestionSteps())
                    .orElse(List.of())
                    .stream()
                    .filter(
                            ingestionStep -> ingestionStep.getStatus().equals("FAILED")
                    )
                    .collect(Collectors.toList());

            for (Upload.IngestionStep ingestionStep : failedIngestionSteps) {
                LOGGER.warning(String.format(
                        "%s ingestion step %s due to:%s%n",
                        ingestionStep.getName(),
                        ingestionStep.getStatus().toLowerCase(),
                        Optional.ofNullable(ingestionStep.getErrors()).orElse(List.of()).stream().map(
                                ingestionError -> String.format(
                                        "%n\t%s:%n\t\t%s",
                                        ingestionError.getCode(),
                                        ingestionError.getMessage()
                                )
                        ).collect(Collectors.joining())
                ));
                if (ingestionStep.getLogUrl() != null && !ingestionStep.getLogUrl().isEmpty()) {
                    StringBuilder logBuffer = new StringBuilder();
                    try {
                        URLConnection urlConnection = new URL(ingestionStep.getLogUrl()).openConnection();
                        urlConnection.setDoInput(true);
                        try (InputStream inputStream = urlConnection.getInputStream();
                             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                             BufferedReader responseBodyReader = new BufferedReader(inputStreamReader)) {
                            for (int c = responseBodyReader.read(); c >= 0; c = responseBodyReader.read()) {
                                logBuffer.append((char) c);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.severe(String.format(
                                "Failed to retrieve ingestion log file at URL %s.%n",
                                ingestionStep.getLogUrl()
                        ));
                    }
                    LOGGER.warning(String.format(
                            "Ingestion log:%n%s%n",
                            logBuffer
                    ));
                }
            }
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
