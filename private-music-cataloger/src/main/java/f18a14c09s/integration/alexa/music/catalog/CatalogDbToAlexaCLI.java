package f18a14c09s.integration.alexa.music.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.*;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.alexa.smapi.CatalogContentUploadClient;
import f18a14c09s.integration.alexa.smapi.data.Catalog;
import f18a14c09s.integration.alexa.smapi.data.Upload;

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
    private static boolean uploadDeletions = false;

    private static <E> List<E> concat(
            Collection<E> c1,
            Collection<E> c2
    ) {
        return Stream.concat(
                c1.stream(),
                c2.stream()
        ).collect(Collectors.toList());
    }

    private static List<MusicEntityDeletion> filterDeletedEntityAttributes(
            Collection<? extends BaseEntity> entities
    ) {
        return entities.stream().map(
                CatalogDbToAlexaCLI::filterDeletedEntityAttributes
        ).collect(Collectors.toList());
    }

    private static MusicEntityDeletion filterDeletedEntityAttributes(
            BaseEntity entity
    ) {
        try {
            return JSON_MAPPER.readValue(
                    JSON_MAPPER.writeValueAsString(
                            entity
                    ),
                    MusicEntityDeletion.class
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
            LOGGER.info(String.format("%s tracks found.", tracks.size()));
            MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
            trackCatalog.setEntities(tracks);
            trackCatalog.setLocales(List.of(Locale.en_US()));
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
            albumCatalog.setLocales(List.of(Locale.en_US()));
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
        }

        if (uploadDeletions) {
            List<Track> trackDeletions = catalogDAO.listTrackDeletions();
            LOGGER.info(String.format("%s track deletions found.", trackDeletions.size()));
            MusicDeletionCatalogUpload trackDeletionCatalogUpload = new MusicDeletionCatalogUpload();
            trackDeletionCatalogUpload.setVersion(2.0);
            trackDeletionCatalogUpload.setType(CatalogTypeName.AMAZON_MUSIC_RECORDING);
            trackDeletionCatalogUpload.setLocales(List.of(Locale.en_US()));
            trackDeletionCatalogUpload.setEntities(
                    filterDeletedEntityAttributes(
                            trackDeletions
                    )
            );
            //
            List<Album> albumDeletions = catalogDAO.listAlbumDeletions();
            LOGGER.info(String.format(
                    "%s album deletions found.",
                    albumDeletions.size()
            ));
            MusicDeletionCatalogUpload albumDeletionCatalogUpload = new MusicDeletionCatalogUpload();
            albumDeletionCatalogUpload.setVersion(2.0);
            albumDeletionCatalogUpload.setType(CatalogTypeName.AMAZON_MUSIC_ALBUM);
            albumDeletionCatalogUpload.setLocales(List.of(Locale.en_US()));
            albumDeletionCatalogUpload.setEntities(
                    filterDeletedEntityAttributes(
                            albumDeletions
                    )
            );
            //
            List<Artist> artistDeletions = catalogDAO.listArtistDeletions();
            LOGGER.info(String.format(
                    "%s artist deletions found.",
                    artistDeletions.size()
            ));
            MusicDeletionCatalogUpload artistDeletionCatalogUpload = new MusicDeletionCatalogUpload();
            artistDeletionCatalogUpload.setVersion(2.0);
            artistDeletionCatalogUpload.setType(CatalogTypeName.AMAZON_MUSIC_GROUP);
            artistDeletionCatalogUpload.setLocales(List.of(Locale.en_US()));
            artistDeletionCatalogUpload.setEntities(
                    filterDeletedEntityAttributes(
                            artistDeletions
                    )
            );

            for (MusicDeletionCatalogUpload catalog : List.of(
                    trackDeletionCatalogUpload,
                    albumDeletionCatalogUpload,
                    artistDeletionCatalogUpload
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
        }

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
    ) throws IOException {
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
//                if (logBuffer.length() <= 1000) {
                LOGGER.warning(String.format(
                        "Ingestion log:%n%s%n",
                        logBuffer
                ));
//                } else {
//                    File localLogFile = File.createTempFile("log", ".txt");
//                    LOGGER.warning(String.format(
//                            "Ingestion log saved to:%n%s%n",
//                            localLogFile.getAbsolutePath()
//                    ));
//                    try (FileWriter fileWriter = new FileWriter(
//                            localLogFile
//                    );
//                         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
//                        bufferedWriter.write(logBuffer.toString());
//                    }
//                }
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
