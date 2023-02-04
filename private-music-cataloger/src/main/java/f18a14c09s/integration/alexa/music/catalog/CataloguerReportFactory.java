package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.mp3.TrackMetadata;

import java.util.*;
import java.util.stream.Collectors;

public class CataloguerReportFactory {
    public static final List<String> DEFAULT_HEADER = List.of(
            "Entity Type",
            "Entity ID",
            "Artist Name",
            "Album Name",
            "Track Name",
            "Track Number",
            "Total Artist Matches",
            "Total Album Matches"
    );

    public static RowBuilder rowBuilder() {
        return new RowBuilder();
    }

    public static List<String> toReportRow(Artist artist) {
        return CataloguerReportFactory.rowBuilder()
                .entityType(EntityType.ARTIST)
                .entityId(artist.getId())
                .artistName(artist.getNames().get(0).getValue())
                .build();
    }

    public static List<String> toReportRow(Album album) {
        List<ArtistReference> artistReferences = Optional.ofNullable(album.getArtists())
                .orElse(List.of());
        return CataloguerReportFactory.rowBuilder()
                .entityType(EntityType.ALBUM)
                .entityId(album.getId())
                .artistName(artistReferences
                        .stream()
                        .map(BaseEntityReference::getNames)
                        .flatMap(Collection::stream)
                        .findAny()
                        .map(EntityName::getValue)
                        .orElse(null))
                .albumName(Optional.ofNullable(album.getNames())
                        .orElse(List.of())
                        .stream()
                        .map(EntityName::getValue)
                        .findAny()
                        .orElse(null))
                .totalArtistMatches(artistReferences.size())
                .build();
    }

    public static List<String> toReportRow(Track track) {
        List<ArtistReference> artistReferences = Optional.ofNullable(track.getArtists())
                .orElse(List.of());
        List<AlbumReference> albumReferences = Optional.ofNullable(track.getAlbums())
                .orElse(List.of());
        return CataloguerReportFactory.rowBuilder()
                .entityType(EntityType.TRACK)
                .entityId(track.getId())
                .artistName(artistReferences
                        .stream()
                        .map(BaseEntityReference::getNames)
                        .flatMap(Collection::stream)
                        .findAny()
                        .map(EntityName::getValue)
                        .orElse(null))
                .albumName(albumReferences
                        .stream()
                        .map(BaseEntityReference::getNames)
                        .flatMap(Collection::stream)
                        .findAny()
                        .map(EntityName::getValue)
                        .orElse(null))
                .trackName(track.getNames().get(0).getValue())
                .trackNumber(track.getNaturalOrder())
                .totalArtistMatches(artistReferences.size())
                .totalAlbumMatches(albumReferences.size())
                .build();
    }

    public static List<String> toReportRow(TrackMetadata trackMetadata) {
        return CataloguerReportFactory.rowBuilder()
                .entityType(String.format("%s-Metadata", EntityType.TRACK))
                .artistName(Optional.ofNullable(trackMetadata.getDistinctArtistNames())
                        .map(Map::keySet)
                        .orElse(Set.of())
                        .stream()
                        .findAny()
                        .orElse(null))
                .albumName(trackMetadata.getAlbum())
                .trackName(trackMetadata.getTitle())
                .trackNumber(trackMetadata.getTrackNumber())
                .build();
    }

    public static class RowBuilder {
        private Map<Integer, String> rowData = new HashMap<>();

        private void setCell(int index, Object value) {
            rowData.put(index, value == null ? "" : value.toString());
        }

        public List<String> build() {
            List<String> row = DEFAULT_HEADER.stream().map(columnName -> "").collect(Collectors.toList());
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
    }
}
