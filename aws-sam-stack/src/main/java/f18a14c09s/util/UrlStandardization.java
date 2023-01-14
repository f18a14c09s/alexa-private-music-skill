package f18a14c09s.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UrlStandardization {
    private static String encodeUriComponent(String uriComponent) {
        return URLEncoder.encode(uriComponent, StandardCharsets.UTF_8);
    }

    public static String encodeRawPathComponents(String url) {
        Matcher urlPrefixMatcher = Pattern.compile(
                "^https://[^/]+/"
        ).matcher(url);
        List<MatchResult> urlPrefixMatches = urlPrefixMatcher.results().collect(Collectors.toList());

        if (urlPrefixMatches.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "URL %s does not start with https://<hostname>/.",
                            url
                    )
            );
        }

        String urlPrefix = urlPrefixMatches.get(0).group(0);
        String urlSuffix = urlPrefixMatcher.replaceFirst("");

        String[] urlPathComponents = urlSuffix.split("/");
        String encodedUrlPath = Arrays.stream(urlPathComponents).map(
                UrlStandardization::encodeUriComponent
        ).collect(Collectors.joining("/"));

        return urlPrefix + encodedUrlPath;
    }
}
