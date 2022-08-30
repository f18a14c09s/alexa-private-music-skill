package f18a14c09s.integration.alexa.smapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.smapi.data.*;
import f18a14c09s.util.StringObjectMap;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CatalogContentUploadClient {
    public static final String DEFAULT_API_ENDPOINT = (
            "https://api.amazonalexa.com"
    );
    private static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getSimpleName()
    );

    private String accessToken;
    private ObjectMapper jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public CatalogContentUploadClient(
            String accessToken
    ) {
        this.accessToken = accessToken;
    }

    private <R> R readJson(
            InputStream inputStream,
            Class<R> clazz
    ) {
        try (inputStream) {
            return jsonMapper.readValue(
                    inputStream,
                    clazz
            );
        } catch (IOException e) {
            LOGGER.log(
                    Level.FINE,
                    "Failed to read JSON from input stream.",
                    e
            );
            return null;
        }
    }

    private <R> R callAlexaApi(
            ApiMethod apiMethod,
            Map<String, String> pathParams,
            Map<String, List<String>> query,
            Object requestBody,
            Class<R> responseType
    ) throws IOException {
        String path = apiMethod.getPath();
        if (pathParams != null) {
            for (String pathParamName : pathParams.keySet()) {
                path = path.replace(
                        String.format(
                                "{%s}",
                                pathParamName
                        ),
                        pathParams.get(pathParamName)
                );
            }
        }
        String queryString = "";
        if (query != null && !query.isEmpty()) {
            queryString = "?" + query.keySet().stream().flatMap(
                    paramName -> query.get(paramName).stream().map(
                            paramValue -> String.format(
                                    "%s=%s",
                                    paramName,
                                    URLEncoder.encode(
                                            (paramValue == null ? "" : paramValue),
                                            StandardCharsets.UTF_8
                                    )
                            )
                    )
            ).collect(Collectors.joining("&"));
        }
        String url = String.format(
                "%s%s%s",
                DEFAULT_API_ENDPOINT,
                path,
                queryString
        );
        LOGGER.info(String.format(
                "%s %s",
                apiMethod.getMethod(),
                url
        ));
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        urlConnection.setRequestMethod(apiMethod.getMethod());
        urlConnection.setRequestProperty(
                "Authorization",
                String.format(
                        "Bearer %s",
                        accessToken
                )
        );
        urlConnection.setDoOutput(true);
        if (requestBody != null) {
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );
            try (OutputStream requestStream = urlConnection.getOutputStream()) {
                jsonMapper.writeValue(
                        requestStream,
                        requestBody
                );
            }
        }
        try {
            int responseCode = urlConnection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(String.format(
                        "Alexa SMAPI responded with HTTP %s %s:%n%s.",
                        responseCode,
                        urlConnection.getResponseMessage(),
                        jsonMapper.writeValueAsString(
                                readJson(
                                        urlConnection.getErrorStream(),
                                        StringObjectMap.class
                                )
                        )
                ));
            }
            return readJson(
                    urlConnection.getInputStream(),
                    responseType
            );
        } finally {
            urlConnection.disconnect();
        }
    }

    public List<Catalog> listCatalogs(
            String vendorId
    ) throws IOException {
        Map<String, List<String>> query = new HashMap<>();
        query.put("vendorId", List.of(vendorId));
        query.put("maxResults", List.of(Integer.toString(50)));
        List<Catalog> allCatalogs = new ArrayList<>();
        for (; ; ) {
            CatalogListing listing = callAlexaApi(
                    ApiMethod.LIST_CATALOGS,
                    null,
                    query,
                    null,
                    CatalogListing.class
            );
            if (listing.getCatalogs() != null) {
                allCatalogs.addAll(listing.getCatalogs());
            }
            if (listing.getNextToken() == null) {
                break;
            }
            query.put("nextToken", List.of(listing.getNextToken()));
        }
        return allCatalogs;
    }

    public String createCatalog(
            AbstractCatalog sourceCatalog,
            String vendorId
    ) throws IOException {
        CatalogCreationRequest targetCatalog = new CatalogCreationRequest();
        targetCatalog.setTitle(String.format(
                "%s Catalog %s",
                sourceCatalog.getType(),
                System.currentTimeMillis()
        ));
        targetCatalog.setType(sourceCatalog.getType());
        targetCatalog.setUsage(sourceCatalog.getDefaultUsage());
        targetCatalog.setVendorId(vendorId);
        return callAlexaApi(
                ApiMethod.CREATE_CATALOG,
                null,
                null,
                targetCatalog,
                String.class
        );
    }

    public void associateCatalogWithSkill(
            String skillId,
            String catalogId
    ) throws IOException {
        callAlexaApi(
                ApiMethod.ASSOCIATE_CATALOG_WITH_SKILL,
                Map.of(
                        "skillId",
                        skillId,
                        "catalogId",
                        catalogId
                ),
                null,
                null,
                null
        );
    }

    public CreateCatalogUploadResponse createUpload(
            String catalogId,
            Integer numberOfUploadParts
    ) throws IOException {
        return callAlexaApi(
                ApiMethod.CREATE_UPLOAD,
                Map.of(
                        "catalogId",
                        catalogId
                ),
                null,
                (
                        numberOfUploadParts == null ?
                                null :
                                Map.of(
                                        "numberOfUploadParts",
                                        numberOfUploadParts
                                )
                ),
                CreateCatalogUploadResponse.class
        );
    }

    public StringObjectMap completeUpload(
            String catalogId,
            String uploadId,
            List<Map<String, Object>> partETags
    ) throws IOException {
        return callAlexaApi(
                ApiMethod.COMPLETE_UPLOAD,
                Map.of(
                        "catalogId",
                        catalogId,
                        "uploadId",
                        uploadId
                ),
                null,
                (
                        partETags == null ?
                                null :
                                Map.of(
                                        "partETags",
                                        partETags
                                )
                ),
                StringObjectMap.class
        );
    }

    public List<Upload> listUploads(
            String catalogId
    ) throws IOException {
        Map<String, List<String>> query = new HashMap<>();
        query.put("maxResults", List.of(Integer.toString(50)));
        List<Upload> allUploads = new ArrayList<>();
        for (; ; ) {
            UploadListing listing = callAlexaApi(
                    ApiMethod.LIST_UPLOADS,
                    Map.of(
                            "catalogId",
                            catalogId
                    ),
                    query,
                    null,
                    UploadListing.class
            );
            if (listing.getUploads() != null) {
                allUploads.addAll(listing.getUploads());
            }
            if (listing.getNextToken() == null) {
                break;
            }
            query.put("nextToken", List.of(listing.getNextToken()));
        }
        return allUploads;
    }

    public Upload getUpload(
            String catalogId,
            String uploadId
    ) throws IOException {
        return callAlexaApi(
                ApiMethod.GET_UPLOAD,
                Map.of(
                        "catalogId",
                        catalogId,
                        "uploadId",
                        uploadId
                ),
                null,
                null,
                Upload.class
        );
    }

    public void uploadCatalogContent(
            String catalogId,
            AbstractCatalog... catalogContentList
    ) throws IOException {
        CreateCatalogUploadResponse uploadCreationResponse = createUpload(
                catalogId,
                catalogContentList.length
        );
        String[] etags = new String[catalogContentList.length];
        for (int i = 0; i < catalogContentList.length; i++) {
            AbstractCatalog catalogContent = catalogContentList[i];
            CreateCatalogUploadResponse.PresignedUploadPart presignedUploadPart = uploadCreationResponse.getPresignedUploadParts()
                    .get(i);
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(presignedUploadPart.getUrl()).openConnection();
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );
            try (
                    OutputStream requestBody = urlConnection.getOutputStream();
                    BufferedOutputStream bufferedRequestBody = new BufferedOutputStream(requestBody)
            ) {
                jsonMapper.writeValue(
                        bufferedRequestBody,
                        catalogContent
                );
            }
            int responseCode = urlConnection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(String.format(
                        "Pre-signed S3 URL responded with HTTP %s %s:%n%s.",
                        responseCode,
                        urlConnection.getResponseMessage(),
                        jsonMapper.writeValueAsString(
                                readJson(
                                        urlConnection.getErrorStream(),
                                        StringObjectMap.class
                                )
                        )
                ));
            }
            etags[i] = urlConnection.getHeaderField("ETag");
        }
        completeUpload(
                catalogId,
                uploadCreationResponse.getId(),
                IntStream.range(0, etags.length).mapToObj(
                        partIndex -> Map.<String, Object>of(
                                "eTag",
                                etags[partIndex],
                                "partNumber",
                                partIndex + 1
                        )
                ).collect(Collectors.toList())
        );
    }
}
