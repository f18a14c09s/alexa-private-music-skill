package f18a14c09s.integration.alexa.music.catalog.reporting;

import f18a14c09s.integration.alexa.music.entities.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RowBuilder {
    private Map<Integer, String> rowData = new HashMap<>();

    private void setCell(int index, Object value) {
        rowData.put(index, value == null ? "" : value.toString());
    }

    public List<String> build() {
        List<String>
                row =
                CataloguerReportFactory.DEFAULT_HEADER.stream().map(columnName -> "").collect(Collectors.toList());
        for (Integer index : rowData.keySet()) {
            row.set(index, rowData.get(index));
        }
        return row;
    }

    public RowBuilder entityType(EntityType entityType) {
        setCell(0, entityType);
        return this;
    }

    public RowBuilder entityType(String entityType) {
        setCell(0, entityType);
        return this;
    }

    public RowBuilder entityId(String entityId) {
        setCell(1, entityId);
        return this;
    }

    public RowBuilder artistName(String artistName) {
        setCell(2, artistName);
        return this;
    }

    public RowBuilder albumName(String albumName) {
        setCell(3, albumName);
        return this;
    }

    public RowBuilder trackName(String trackName) {
        setCell(4, trackName);
        return this;
    }

    public RowBuilder trackNumber(Long trackNumber) {
        setCell(5, trackNumber);
        return this;
    }

    public RowBuilder totalArtistMatches(int totalArtistMatches) {
        setCell(6, totalArtistMatches);
        return this;
    }

    public RowBuilder totalAlbumMatches(int totalAlbumMatches) {
        setCell(7, totalAlbumMatches);
        return this;
    }

    public RowBuilder existingMatchById(boolean matchFound) {
        setCell(8, matchFound);
        return this;
    }

    public RowBuilder totalExistingMatchesByNames(long totalMatches) {
        setCell(9, totalMatches);
        return this;
    }
}
