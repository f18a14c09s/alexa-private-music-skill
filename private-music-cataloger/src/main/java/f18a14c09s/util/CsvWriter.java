package f18a14c09s.util;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

public interface CsvWriter {
    Pattern REGEX_OF_CHARACTERS_REQUIRING_QUOTES = Pattern.compile("[,\\r\\n]");

    List<String> getHeader();

    Writer getWriter();

    default boolean isAlwaysQuoted() {
        return false;
    }

    default String getLineSeparator() {
        return System.lineSeparator();
    }

    private String quote(String s) {
        String escapedString = s.replace("\"", "\"\"");
        boolean containsCharacterThatRequiresQuotes = REGEX_OF_CHARACTERS_REQUIRING_QUOTES.matcher(s).find();
        if (containsCharacterThatRequiresQuotes || isAlwaysQuoted() || escapedString.length() != s.length()) {
            return "\"" + escapedString + "\"";
        }
        return s;
    }

    default CsvWriter writeHeader() {
        return writeRow(getHeader());
    }

    default CsvWriter writeRow(List<String> row) {
        int totalColumns = row.size();
        try {
            if (totalColumns >= 1) {
                getWriter().write(quote(row.get(0)));
                for (int i = 1; i < totalColumns; i++) {
                    getWriter().write("," + quote(row.get(i)));
                }
            }
            getWriter().write(getLineSeparator());
            getWriter().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}
