package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.data.Country;
import f18a14c09s.integration.alexa.data.Language;
import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.music.data.Art;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.aws.AwsSecretsAdapter;
import f18a14c09s.integration.hibernate.Hbm2DdlAuto;
import org.hibernate.jpa.QueryHints;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.io.IOException;
import java.util.*;

public class CatalogDAO {
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
//    private AwsSecretsAdapter awsSecrets;

    public CatalogDAO() throws IOException {
        this(null);
    }

    public CatalogDAO(Hbm2DdlAuto hbm2DdlAuto) throws IOException {
//        awsSecrets = new AwsSecretsAdapter();
        Map<String, Object> properties = new HashMap<>();
        Map<String, String> xref = new HashMap<>();
        Map<String, Object> secret = null;
        Optional.ofNullable(hbm2DdlAuto)
                .map(Hbm2DdlAuto::value)
                .ifPresent(value -> properties.put("hibernate.hbm2ddl.auto", hbm2DdlAuto.value()));
        xref.put("javax.persistence.jdbc.driver", "databaseDriver");
        xref.put("javax.persistence.jdbc.url", "databaseUrl");
        xref.put("javax.persistence.jdbc.user", "username");
        xref.put("javax.persistence.jdbc.password", "password");
        xref.put("jakarta.persistence.jdbc.driver", "databaseDriver");
        xref.put("jakarta.persistence.jdbc.url", "databaseUrl");
        xref.put("jakarta.persistence.jdbc.user", "username");
        xref.put("jakarta.persistence.jdbc.password", "password");
        xref.put("hibernate.dialect", "hibernateDatabaseDialect");
        for (Map.Entry<String, String> kvp : xref.entrySet()) {
            Optional<String> value = Optional.ofNullable(System.getenv(kvp.getValue()));
            if (!value.isPresent()) {
                if (secret == null) {
//                    String secretArn = System.getenv("DATABASE_SECRET_ARN");
//                    secret = awsSecrets.getSecret(secretArn);
                    secret = new HashMap<>();
                    secret.put("databaseDriver", "org.h2.Driver");
                    secret.put("username", "sa");
                    secret.put("password", "sa-password");
                    secret.put("hibernateDatabaseDialect", "org.hibernate.dialect.H2Dialect");
                    secret.put("databaseUrl", "jdbc:h2:file:./alexa-music-data");
                }
                value = Optional.ofNullable((String) secret.get(kvp.getValue()));
            }
            if (value.isPresent()) {
                properties.put(kvp.getKey(), value.get());
            }
        }
//        String databaseUrl = (String) secret.get("databaseUrlTemplate");
//        for(String key: secret.keySet()) {
//            databaseUrl = databaseUrl.replace("{" + key + "}", (String) secret.get(key));
//        }
//        properties.put("javax.persistence.jdbc.url", databaseUrl);
//        properties.put("jakarta.persistence.schema-generation.scripts.action", "create");
//        properties.put("jakarta.persistence.schema-generation.scripts.create-target", "h2-ddl.sql");
        entityManagerFactory = Persistence.createEntityManagerFactory("alexa-music-data", properties);
        entityManager = entityManagerFactory.createEntityManager();
    }

    public void save(AbstractCatalog catalog) {
        saveEntity(catalog);
    }

    public void save(Locale locale) {
        saveEntity(locale);
    }

    public void save(Art art) {
        saveEntity(art);
    }

    private void saveEntity(Object entity) {
        if (!entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
        }
        entityManager.persist(entity);
    }

    public void close(boolean commit) {
        if (commit) {
            commit();
        }
        entityManager.close();
        entityManagerFactory.close();
    }

    public void commit() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().commit();
        }
    }

    public List<ArtistReference> findAllArtistReferences() {
        return findAll(ArtistReference.class);
    }

    public List<AlbumReference> findAllAlbumReferences() {
        return findAll(AlbumReference.class);
    }

    private <E> List<E> findAll(Class<E> clazz) {
        CriteriaQuery<E> criteria = entityManager.getCriteriaBuilder().createQuery(clazz);
        criteria.distinct(true).from(clazz);
        return entityManager.createQuery(criteria).getResultList();
    }

    public Long count(Class<?> clazz) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        Root root = criteria.from(clazz);
        return entityManager.createQuery(criteria.select(cb.count(root))).getSingleResult();
    }

    public Locale findLocale(Country country, Language language) {
        return entityManager.createQuery("SELECT o FROM Locale o WHERE o.country = :country AND o.language = :language",
                Locale.class).setParameter("country", country).setParameter("language", language).getSingleResult();
    }

    public <E extends BaseEntity> E findEntity(Class<E> clazz, String id) {
        return entityManager.find(clazz, id);
    }

    public List<Track> findArtistTracks(String artistId) {
        return entityManager.createQuery(
                "SELECT track FROM Track track JOIN track.artists artist JOIN track.albums album JOIN album.names albumName WHERE artist.id = :artistid ORDER BY album.naturalOrder, albumName.value, track.naturalOrder",
                Track.class).setParameter("artistid", artistId).getResultList();
    }

    public List<Track> findAlbumTracks(String albumId) {
        return entityManager.createQuery(
                "SELECT track FROM Track track JOIN track.albums album WHERE album.id = :albumid ORDER BY track.naturalOrder",
                Track.class).setParameter("albumid", albumId).getResultList();
    }

    public Map<EntityType, Map<List<String>, String>> getCataloguedEntityIdsByTypeAndNaturalKey() {
        Map<EntityType, Map<List<String>, String>> retval = new HashMap<>();
        Query query = entityManager.createNativeQuery(
                "SELECT entity_id, entity_type, artist_name, album_name, song_url" +
                        " FROM catalogued_music_entities"
        );
        ((List<?>) query.getResultList()).stream().map(Object[].class::cast).forEach(row -> {
            String entityId = (String) row[0];
            EntityType entityType = EntityType.valueOf((String) row[1]);
            List<String> key;
            switch (entityType) {
                case ARTIST: {
                    key = Collections.singletonList((String) row[2]);
                    break;
                }
                case ALBUM: {
                    key = Arrays.asList((String) row[2], (String) row[3]);
                    break;
                }
                case TRACK: {
                    key = Collections.singletonList((String) row[4]);
                    break;
                }
                default: {
                    throw new RuntimeException("Unexpected entity type.");
                }
            }
            retval.computeIfAbsent(entityType, k -> new HashMap<>()).put(key, entityId);
        });
        return retval;
    }

    public List<AbstractCatalog> findAllCatalogs() {
        return entityManager.createQuery("SELECT o FROM AbstractCatalog o ORDER BY o.type, o.id", AbstractCatalog.class)
                .setHint(QueryHints.HINT_FETCH_SIZE, 1000)
                .getResultList();
    }
}
