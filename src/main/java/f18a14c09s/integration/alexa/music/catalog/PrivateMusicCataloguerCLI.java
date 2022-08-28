package f18a14c09s.integration.alexa.music.catalog;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class PrivateMusicCataloguerCLI {
    @Getter
    public enum CommandLineArgument {
        SOURCE_S3_BUCKET_NAME(
                "--src-s3-bucket",
                List.of(UnaryOperator.identity())
        ),
        SOURCE_S3_PREFIX(
                "--src-s3-prefix",
                List.of(UnaryOperator.identity())
        ),
        DESTINATION_STR_STR_DYNAMODB_TABLE_NAME(
                "--dest-str-str-dynamodb-table",
                List.of(UnaryOperator.identity())
        ),
        DESTINATION_STR_NUM_DYNAMODB_TABLE_NAME(
                "--dest-str-num-dynamodb-table",
                List.of(UnaryOperator.identity())
        ),
        MUSIC_BASE_URL(
                "--base-url",
                List.of(UnaryOperator.identity())
        ),
        IMAGE_BASE_URL(
                "--image-base-url",
                List.of(UnaryOperator.identity())
        );
        public final String longArgumentName;
        public final List<Function<String, ?>> argValueParsers;

        CommandLineArgument(String longArgumentName, List<Function<String, ?>> argValueParsers) {
            this.longArgumentName = longArgumentName;
            this.argValueParsers = Collections.unmodifiableList(
                    argValueParsers
            );
        }
    }

    public static void main(String... args) throws IOException, NoSuchAlgorithmException {
        PrivateMusicCataloguer cli = newCataloguer(parseCommandLineArguments(args));
        cli.catalogMusic();
    }

    private static PrivateMusicCataloguer newCataloguer(Map<CommandLineArgument, List<Object>> args) throws
            IOException,
            NoSuchAlgorithmException {
        List<String> validationErrors = new ArrayList<>();
        for (CommandLineArgument requiredArg :
                Set.of(
                        CommandLineArgument.SOURCE_S3_BUCKET_NAME,
                        CommandLineArgument.SOURCE_S3_PREFIX,
                        CommandLineArgument.MUSIC_BASE_URL,
                        CommandLineArgument.IMAGE_BASE_URL,
                        CommandLineArgument.DESTINATION_STR_STR_DYNAMODB_TABLE_NAME,
                        CommandLineArgument.DESTINATION_STR_NUM_DYNAMODB_TABLE_NAME
                )) {
            if (Optional.ofNullable(args.get(requiredArg)).filter(Predicate.not(List::isEmpty)).map(list -> list.get(0)).filter(value -> !(value instanceof String) || !String.class.cast(value).trim().isEmpty()).isEmpty()) {
                validationErrors.add(String.format("Argument %s is required.", requiredArg.longArgumentName));
            }
        }
        String sourceS3BucketName = Optional.ofNullable(
                args.get(CommandLineArgument.SOURCE_S3_BUCKET_NAME)
        ).map(argValues -> argValues.get(0)).map(String.class::cast).orElse(null);
        String sourceS3Prefix = Optional.ofNullable(
                args.get(CommandLineArgument.SOURCE_S3_BUCKET_NAME)
        ).map(argValues -> argValues.get(0)).map(String.class::cast).orElse(null);
        String baseUrl = Optional.ofNullable(
                args.get(CommandLineArgument.MUSIC_BASE_URL)
        ).map(argValues -> argValues.get(0)).map(String.class::cast).orElse(null);
        String imageBaseUrl = Optional.ofNullable(
                args.get(CommandLineArgument.IMAGE_BASE_URL)
        ).map(argValues -> argValues.get(0)).map(String.class::cast).orElse(null);
        String destStrStrDynamodbTableName = Optional.ofNullable(
                args.get(CommandLineArgument.DESTINATION_STR_STR_DYNAMODB_TABLE_NAME)
        ).map(argValues -> argValues.get(0)).map(String.class::cast).orElse(null);
        String destStrNumDynamodbTableName = Optional.ofNullable(
                args.get(CommandLineArgument.DESTINATION_STR_NUM_DYNAMODB_TABLE_NAME)
        ).map(argValues -> argValues.get(0)).map(String.class::cast).orElse(null);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException(validationErrors.toString());
        }
        return new PrivateMusicCataloguer(
                sourceS3BucketName,
                sourceS3Prefix,
                baseUrl,
                imageBaseUrl,
                destStrStrDynamodbTableName,
                destStrNumDynamodbTableName
        );
    }

    private static Map<CommandLineArgument, List<Object>> parseCommandLineArguments(String... args) {
        Map<String, CommandLineArgument> validCommandLineArguments = Arrays.stream(CommandLineArgument.values()).collect(Collectors.toMap(CommandLineArgument::getLongArgumentName, Function.identity()));
        Map<CommandLineArgument, List<Object>> retval = new HashMap<>();
        for (int i = 0; args != null && i < args.length; ) {
            String argName = args[i];
            CommandLineArgument arg = validCommandLineArguments.get(argName);
            if (arg == null) {
                throw new IllegalArgumentException(String.format("Argument %s not recognized.", argName));
            }
            List<Function<String, ?>> argValueParsers = arg.argValueParsers;
            List<Object> argValues = new ArrayList<>();
            for (int j = 0; argValueParsers != null && j < argValueParsers.size(); j++, i++) {
                if (i >= args.length) {
                    throw new IllegalArgumentException(String.format(
                            "Argument %s has too few values.  Required: %s.  Actual: %s.",
                            arg.longArgumentName,
                            j,
                            argValueParsers.size()));
                }
                argValues.add(argValueParsers.get(j).apply(args[i]));
            }
            retval.put(arg, argValues);
        }
        return retval;
    }
}
