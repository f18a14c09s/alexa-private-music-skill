package f18a14c09s.integration.alexa.music.catalog;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.*;

public class PrivateMusicCataloguerCLI {

    public static final Map<String, List<Function<String, ?>>> VALID_COMMAND_LINE_ARGUMENTS =
            getValidCommandLineArguments();

    public static final String ARG_NAME_SRC_DIR = "--src-dir";
    public static final String ARG_NAME_DEST_DIR = "--dest-dir";
    public static final String ARG_NAME_BASE_URL = "--base-url";
    public static final String ARG_NAME_IMAGE_BASE_URL = "--image-base-url";
    public static final String ARG_NAME_RESET = "--reset";
    public static final String ARG_NAME_WRITE_TO_DB = "--write-to-db";

    public static void main(String... args) throws IOException, NoSuchAlgorithmException {
        PrivateMusicCataloguer cli = newCataloguer(parseCommandLineArguments(args));
        cli.catalogMusic();
    }

    private static PrivateMusicCataloguer newCataloguer(Map<String, List<Object>> args) throws
            IOException,
            NoSuchAlgorithmException {
        List<String> validationErrors = new ArrayList<>();
        File src = null, dest = null;
        String baseUrl = null;
        String imageBaseUrl = null;
        boolean reset = args.containsKey(ARG_NAME_RESET);
        boolean writeToDb = args.containsKey(ARG_NAME_WRITE_TO_DB);
        if (args.containsKey(ARG_NAME_SRC_DIR)) {
            src = new File((String) args.get(ARG_NAME_SRC_DIR).get(0));
        } else {
            validationErrors.add(String.format("Argument %s is required.", ARG_NAME_SRC_DIR));
        }
        if (args.containsKey(ARG_NAME_BASE_URL)) {
            baseUrl = (String) args.get(ARG_NAME_BASE_URL).get(0);
        } else {
            validationErrors.add(String.format("Argument %s is required.", ARG_NAME_BASE_URL));
        }
        if (args.containsKey(ARG_NAME_IMAGE_BASE_URL)) {
            imageBaseUrl = (String) args.get(ARG_NAME_IMAGE_BASE_URL).get(0);
        } else {
            validationErrors.add(String.format("Argument %s is required.", ARG_NAME_IMAGE_BASE_URL));
        }
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException(validationErrors.toString());
        }
        if (args.containsKey(ARG_NAME_DEST_DIR)) {
            dest = new File((String) args.get(ARG_NAME_DEST_DIR).get(0));
        }
        return new PrivateMusicCataloguer(src, dest, baseUrl, reset, imageBaseUrl, writeToDb);
    }

    private static Map<String, List<Object>> parseCommandLineArguments(String... args) {
        Map<String, List<Object>> retval = new HashMap<>();
        for (int i = 0; args != null && i < args.length; ) {
            String argName = args[i++];
            if (!VALID_COMMAND_LINE_ARGUMENTS.containsKey(argName)) {
                throw new IllegalArgumentException(String.format("Argument %s not recognized.", argName));
            }
            List<Function<String, ?>> argValueParsers = VALID_COMMAND_LINE_ARGUMENTS.get(argName);
            List<Object> argValues = new ArrayList<>();
            for (int j = 0; argValueParsers != null && j < argValueParsers.size(); j++, i++) {
                if (i >= args.length) {
                    throw new IllegalArgumentException(String.format(
                            "Argument %s has too few values.  Required: %s.  Actual: %s.",
                            argName,
                            j,
                            argValueParsers.size()));
                }
                argValues.add(argValueParsers.get(j).apply(args[i]));
            }
            retval.put(argName, argValues);
        }
        return retval;
    }

    private static Map<String, List<Function<String, ?>>> getValidCommandLineArguments() {
        Map<String, List<Function<String, ?>>> retval = new HashMap<>();
        retval.put(ARG_NAME_SRC_DIR, Collections.unmodifiableList(Arrays.asList(UnaryOperator.identity())));
        retval.put(ARG_NAME_DEST_DIR, Collections.unmodifiableList(Arrays.asList(UnaryOperator.identity())));
        retval.put(ARG_NAME_BASE_URL, Collections.unmodifiableList(Arrays.asList(UnaryOperator.identity())));
        retval.put(ARG_NAME_IMAGE_BASE_URL, Collections.unmodifiableList(Arrays.asList(UnaryOperator.identity())));
        retval.put(ARG_NAME_RESET, null);
        retval.put(ARG_NAME_WRITE_TO_DB, null);
        return Collections.unmodifiableMap(retval);
    }

}
