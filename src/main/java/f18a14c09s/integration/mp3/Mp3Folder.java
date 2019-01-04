package f18a14c09s.integration.mp3;

import f18a14c09s.integration.alexa.music.catalog.AlbumKey;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.data.ArtSource;
import f18a14c09s.integration.alexa.music.data.ArtSourceSize;
import f18a14c09s.integration.alexa.music.entities.Album;
import f18a14c09s.integration.alexa.music.entities.Artist;
import f18a14c09s.integration.alexa.music.entities.EntityName;
import f18a14c09s.integration.alexa.music.entities.Popularity;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static f18a14c09s.util.CollectionUtil.asArrayList;

public class Mp3Folder {
    @Getter
    private List<TrackMetadata> mp3s;

    @Getter(AccessLevel.PRIVATE)
    private List<ImageMetadata> images;

    @Getter
    private List<Mp3Folder> children;

    @Getter(AccessLevel.PRIVATE)
    private int hierarchyLevel;

    @Getter
    private Art art;

//    @Getter
//    private Map<String, Artist> artists;
//
//    @Getter
//    private Map<AlbumKey, Album> albums;

    public Mp3Folder(List<TrackMetadata> mp3s,
                     List<ImageMetadata> images,
                     int hierarchyLevel,
                     List<Mp3Folder> children) {
        this.mp3s = mp3s;
        this.images = images;
        this.hierarchyLevel = hierarchyLevel;
        this.children = children;
        this.art = toArt(images);
//        this.artists = getUniqueArtistNames(mp3s, children).stream()
//                .collect(Collectors.toMap(UnaryOperator.identity(), this::newArtistEntity));
//        Map<AlbumKey, Album> albums = new HashMap<>();
//        children.stream().map(Mp3Folder::getAlbums).forEach(albums::putAll);
//        getUniqueAlbumNames(mp3s, children).stream()
//                .forEach(album -> albums.computeIfAbsent(album, this::newAlbumEntity));
//        this.albums = albums;
//        Map<String, Artist> artists = new HashMap<>();
//        children.stream().map(Mp3Folder::getArtists).forEach(artists::putAll);
//        getUniqueArtistNames(mp3s, children).forEach(artist -> artists.computeIfAbsent(artist, this::newArtistEntity));
//        this.artists = artists;
    }

    private static Map<ArtSourceSize, ImageMetadata> getExactlyOneImagePerArtSourceSize(List<ImageMetadata> images) {
        if (images == null || images.isEmpty()) {
            return null;
        } else {
            Map<ArtSourceSize, ImageMetadata> imagesBySize = images.stream()
                    .filter(image -> image.getArtSourceSize() != null)
                    .collect(Collectors.groupingBy(ImageMetadata::getArtSourceSize))
                    .values()
                    .stream()
                    .map(Iterable::iterator)
                    .map(Iterator::next)
                    .collect(Collectors.toMap(ImageMetadata::getArtSourceSize, UnaryOperator.identity()));
            for (ArtSourceSize size : Arrays.stream(ArtSourceSize.values())
                    .filter(size -> !imagesBySize.containsKey(size))
                    .collect(Collectors.toSet())) {
                for (ArtSourceSize altSize : size.getAlternateSizesByPriority()) {
                    if (imagesBySize.containsKey(altSize)) {
                        imagesBySize.put(size, imagesBySize.get(altSize));
                        break;
                    }
                }
            }
            return imagesBySize;
        }
    }

    private static Art toArt(List<ImageMetadata> images) {
        return Optional.ofNullable(getExactlyOneImagePerArtSourceSize(images))
                .map(Map::entrySet)
                .map(Collection::stream)
                .map(stream -> stream.map(entry -> new ArtSource(entry.getValue().getUrl(),
                        entry.getKey(),
                        entry.getValue().getWidth(),
                        entry.getValue().getHeight())).collect(Collectors.toList()))
                .map(Art::new)
                .orElse(null);
    }

    public boolean isLikelyAlbumFolder() {
        return (getMp3s() != null && !getMp3s().isEmpty()) || (getHierarchyLevel() >= 2 && getChildren() != null &&
                getChildren().stream().anyMatch(Mp3Folder::isLikelyAlbumFolder));
    }

    public boolean isLikelyArtistFolder() {
        return !isLikelyAlbumFolder() && getHierarchyLevel() == 1 && getChildren() != null &&
                getChildren().stream().anyMatch(Mp3Folder::isLikelyAlbumFolder);
    }

    private static Set<AlbumKey> getUniqueAlbumNames(List<TrackMetadata> mp3s, List<Mp3Folder> children) {
        Set<AlbumKey> retval = new HashSet<>();
        Optional.ofNullable(mp3s)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(track -> new AlbumKey(track.getAuthor(), track.getAlbum()))
                .forEach(retval::add);
//        Optional.ofNullable(children)
//                .map(Collection::stream)
//                .orElse(Stream.empty())
//                .map(Mp3Folder::getUniqueAlbumNames)
//                .forEach(retval::addAll);
        return retval;
    }

    private static Set<String> getUniqueArtistNames(List<TrackMetadata> mp3s, List<Mp3Folder> children) {
        Set<String> retval = new HashSet<>();
        Optional.ofNullable(mp3s)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(TrackMetadata::getAuthor)
                .forEach(retval::add);
//        Optional.ofNullable(children)
//                .map(Collection::stream)
//                .orElse(Stream.empty())
//                .map(Mp3Folder::getUniqueArtistNames)
//                .forEach(retval::addAll);
        return retval;
    }

    public Set<AlbumKey> getUniqueAlbumNames() {
        Set<AlbumKey> retval = new HashSet<>();
        Optional.ofNullable(getMp3s())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(track -> new AlbumKey(track.getAuthor(), track.getAlbum()))
                .forEach(retval::add);
//        Optional.ofNullable(getChildren())
//                .map(Collection::stream)
//                .orElse(Stream.empty())
//                .map(Mp3Folder::getUniqueAlbumNames)
//                .forEach(retval::addAll);
        return retval;
    }

    public Set<String> getUniqueArtistNames() {
        Set<String> retval = new HashSet<>();
        Optional.ofNullable(getMp3s())
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(TrackMetadata::getAuthor)
                .forEach(retval::add);
//        Optional.ofNullable(getChildren())
//                .map(Collection::stream)
//                .orElse(Stream.empty())
//                .map(Mp3Folder::getUniqueArtistNames)
//                .forEach(retval::addAll);
        return retval;
    }

    private Album newAlbumEntity(AlbumKey albumKey) {
        Album retval = new Album();
        retval.setLanguageOfContent(asArrayList("en"));
        retval.setNames(asArrayList(new EntityName("en", albumKey.getAlbumName())));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setReleaseType("Studio Album");
        retval.setLastUpdatedTime(Calendar.getInstance());
        retval.setId(UUID.randomUUID().toString());
//        retval.setLocales(asArrayList(en_US));
        return retval;
    }

    private Artist newArtistEntity(String artistName) {
        Artist retval = new Artist();
        retval.setNames(asArrayList(new EntityName("en", artistName)));
        retval.setPopularity(Popularity.unratedWithNoOverrides());
        retval.setLastUpdatedTime(Calendar.getInstance());
//        retval.setLocales(asArrayList(en_US));
        return retval;
    }
}
