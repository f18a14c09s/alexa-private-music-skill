package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.music.catalog.data.MusicAlbumCatalog;
import f18a14c09s.integration.json.JSONAdapter;
import f18a14c09s.util.ThrowingFunction;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;

/**
 * Alexa Skills Management API (SMAPI) catalog adapter.
 */
public class SmapiCatalogAdapter {
    private String baseUrl;
    private String vendorId;
    private JSONAdapter jsonAdapter = new JSONAdapter();
    private Logger logger = Logger.getLogger(getClass().getName());
    private String accessToken;
    private String refreshToken;

    public static void main(String... args) throws IOException {
        SmapiCatalogAdapter adapter = new SmapiCatalogAdapter(args[0]);
//        MusicGroupCatalog artistCatalog = new JSONAdapter().readValue(new File(
//                        "C:\\Users\\fjohnson\\Music\\Private Music Skill\\C.Users.fjohnson.Music.MP3_Albums-artists.json"),
//                MusicGroupCatalog.class);
//        adapter.createAndUploadCatalog("amzn1.ask.skill.db1fdba6-a1e7-4346-803f-0d01716b436d", artistCatalog);
        MusicAlbumCatalog albumCatalog = new JSONAdapter().readValue(new File(
                        "C:\\Users\\fjohnson\\Music\\Private Music Skill\\C.Users.fjohnson.Music.MP3_Albums-albums.json"),
                MusicAlbumCatalog.class);
        adapter.createAndUploadCatalog(args[1], albumCatalog);
    }

    public SmapiCatalogAdapter(String vendorId) throws IOException {
        this.baseUrl = "https://api.amazonalexa.com/v0";
        this.vendorId = vendorId;
        pickupAlexaSkillsKitCLICredentials();
    }

    public void pickupAlexaSkillsKitCLICredentials() throws IOException {
        File userHome = new File(System.getProperty("user.home"));
        File askCliConfig = new File(new File(userHome, ".ask"), "cli_config");
        Map<?, ?> map;
        try (FileReader fr = new FileReader(askCliConfig)) {
            map = jsonAdapter.readValue(fr, HashMap.class);
        }
        map = (Map<?, ?>) map.get("profiles");
        map = (Map<?, ?>) map.get("default");
        map = (Map<?, ?>) map.get("token");
        this.accessToken = (String) map.get("access_token");
        this.refreshToken = (String) map.get("refresh_token");
    }

    public void createAndUploadCatalog(String skillId, AbstractCatalog catalog) throws IOException {
        String catalogId = getCatalogIfExists(catalog.getType(), catalog.getDefaultUsage(), skillId);
        if (catalogId == null) {
            catalogId = createCatalog(String.format("Alexa Private Music Skill %s", catalog.getType()),
                    catalog.getType(),
                    catalog.getDefaultUsage());
            associateCatalogWithSkill(skillId, catalogId);
        }
        String uploadId = uploadCatalog(catalogId, catalog);
        String uploadStatus = getCatalogUpload(catalogId, uploadId);
        logger.info(String.format("Catalog %s upload %s status: %s.", catalogId, uploadId, uploadStatus));
    }

    private String getCatalogIfExists(String type, String usage, String skillId) throws IOException {
        List<Map<String, Object>> allCatalogs = getAllCatalogs();
        return allCatalogs.stream()
                .filter(catalog -> type.equals(catalog.get("type")) && usage.equals(catalog.get("usage")) &&
                        ((List<String>) catalog.get("associatedSkillIds")).contains(skillId))
                .findAny()
                .map(catalog -> (String) catalog.get("id"))
                .orElse(null);
    }

    private <R> R relativeRequest(String relativeUrl,
                                  String method,
                                  String body,
                                  ThrowingFunction<InputStream, R, IOException> responseTransform) throws IOException {
        return absoluteRequest(String.format("%s/%s", baseUrl, relativeUrl), method, true, body, responseTransform);
    }

    private <R> R absoluteRequest(String absoluteUrl,
                                  String method,
                                  boolean authorizationHeaderNeeded,
                                  String body,
                                  ThrowingFunction<InputStream, R, IOException> responseTransform) throws IOException {
        URL url = new URL(absoluteUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        if (authorizationHeaderNeeded) {
            connection.setRequestProperty("Authorization", accessToken);
        }
        if (body != null) {
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            try (OutputStreamWriter osr = new OutputStreamWriter(connection.getOutputStream(),
                    StandardCharsets.UTF_8)) {
                osr.write(body);
            }
        }
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode <= 299) {
            if (responseTransform != null) {
                try (InputStream responseStream = connection.getInputStream()) {
                    return responseTransform.apply(responseStream);
                }
            }
        } else {
            StringBuilder errorMessage = new StringBuilder();
            try (InputStream errorStream = connection.getErrorStream()) {
                if (errorStream != null) {
                    try (InputStreamReader isr = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
                         BufferedReader br = new BufferedReader(isr)) {
                        for (String line = br.readLine(); line != null; line = br.readLine()) {
                            errorMessage.append(String.format("%n\t%s", line));
                        }
                    }
                }
            }
            throw new IOException(String.format("URL %s returned unexpected HTTP response code %s.%nError message:%s",
                    absoluteUrl,
                    responseCode,
                    errorMessage.toString()));
        }
        return null;
    }

    protected String createCatalog(String title, String type, String usage) throws IOException {
        final String relativeUrl = "catalogs";
        Map<?, ?> response = relativeRequest(relativeUrl,
                "POST",
                String.format("{\"title\": \"%s\", \"type\": \"%s\", \"usage\": \"%s\", \"vendorId\": \"%s\"}",
                        title,
                        type,
                        usage,
                        vendorId),
                jsonAdapter::readObjectMap);
        return (String) response.get("id");
    }

    protected String uploadCatalog(String catalogId, AbstractCatalog catalog) throws IOException {
        final String relativeUrl = String.format("catalogs/%s/uploads", catalogId);
        // Create the upload:
        Map<String, Object> response =
                relativeRequest(relativeUrl, "POST", "{\"numberOfParts\": 1}", jsonAdapter::readObjectMap);
        String uploadId = (String) response.get("id");
        List<Map<String, Object>> presignedUploadParts =
                (List<Map<String, Object>>) response.get("presignedUploadParts");
        String nextUrl = (String) presignedUploadParts.get(0).get("url");
        String body = jsonAdapter.writeValueAsString(catalog);
        // Perform the upload:
        absoluteRequest(nextUrl, "POST", false, body, null);
        // Mark the upload as completed:
        relativeRequest(String.format("catalogs/%s/uploads/%s", catalogId, uploadId), null, null, null);
        return uploadId;
    }

    protected void associateCatalogWithSkill(String skillId, String catalogId) throws IOException {
        final String relativeUrl = String.format("skills/%s/catalogs/%s", skillId, catalogId);
        relativeRequest(relativeUrl, "PUT", null, null);
    }

    public String getCatalogUpload(String catalogId, String uploadId) throws IOException {
        final String relativeUrl = String.format("catalogs/%s/uploads/%s", catalogId, uploadId);
        Map<String, Object> response = relativeRequest(relativeUrl, "GET", null, jsonAdapter::readObjectMap);
        return (String) response.get("status");
    }

    public List<Map<String, Object>> getAllCatalogs() throws IOException {
        List<Map<String, Object>> retval = new ArrayList<>();
        final String relativeUrl = String.format("catalogs?vendorId=%s&maxResults=50", vendorId);
        final String method = "GET";
        for (Map<String, Object> response = relativeRequest(relativeUrl, method, null, jsonAdapter::readObjectMap);
             response != null; response = Boolean.TRUE.equals(response.get("isTruncated")) ?
                relativeRequest(relativeUrl + String.format("&nextToken=%s", response.get("nextToken")),
                        method,
                        null,
                        jsonAdapter::readObjectMap) :
                null) {
            List<Map<String, Object>> catalogs = (List<Map<String, Object>>) response.get("catalogs");
            retval.addAll(catalogs);
        }
        return retval;
    }
}
