package f18a14c09s.integration.alexa.music.catalog.reporting;

import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.mp3.TrackMetadata;

import java.util.*;

public class CataloguerReportFactory {
    public static final List<String> DEFAULT_HEADER = List.of(
            "Entity Type",
            "Entity ID",
            "Artist Name",
            "Album Name",
            "Track Name",
            "Track Number",
            "Total Artist Matches",
            "Total Album Matches",
            "Existing Match by ID",
            "Total Existing Matches by Name(s)"
    );

    public static RowBuilder rowBuilder() {
        return new RowBuilder();
    }

    public static List<String> toReportRow(Artist artist, Artist exactIdMatch, Collection<Artist> matchesByName) {
        return CataloguerReportFactory.rowBuilder()
                .entityType(EntityType.ARTIST)
                .entityId(artist.getId())
                .artistName(artist.getNames().get(0).getValue())
                .existingMatchById(exactIdMatch != null)
                .totalExistingMatchesByNames(Optional.ofNullable(matchesByName).stream().count())
                .build();
    }

    public static List<String> toReportRow(Album album, Album exactIdMatch, Collection<Album> matchesByName) {
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
                .existingMatchById(exactIdMatch != null)
                .totalExistingMatchesByNames(Optional.ofNullable(matchesByName).stream().count())
                .build();
    }

    public static List<String> toReportRow(Track track, Track exactIdMatch, Collection<Track> matchesByName) {
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
                .existingMatchById(exactIdMatch != null)
                .totalExistingMatchesByNames(Optional.ofNullable(matchesByName).stream().count())
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
}
