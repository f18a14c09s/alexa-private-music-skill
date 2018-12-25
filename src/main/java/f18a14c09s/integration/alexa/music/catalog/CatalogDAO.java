package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.data.Locale;
import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.alexa.music.entities.AlbumReference;
import f18a14c09s.integration.alexa.music.entities.ArtistReference;
import f18a14c09s.integration.alexa.music.entities.BaseEntity;
import f18a14c09s.integration.alexa.music.entities.Track;
import f18a14c09s.integration.aws.AwsSecretsAdapter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.util.*;

public class CatalogDAO {
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private AwsSecretsAdapter awsSecrets;

    public CatalogDAO() throws IOException {
        awsSecrets = new AwsSecretsAdapter();
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> secret = awsSecrets.getSecret("alexa-private-music-skill");
        properties.put("javax.persistence.jdbc.driver", secret.get("databaseDriver"));
        properties.put("javax.persistence.jdbc.url", secret.get("databaseUrl"));
        properties.put("javax.persistence.jdbc.user", secret.get("databaseUserId"));
        properties.put("javax.persistence.jdbc.password", secret.get("databasePassword"));
        properties.put("hibernate.dialect", secret.get("hibernateDatabaseDialect"));
        entityManagerFactory = Persistence.createEntityManagerFactory("alexa-music-data", properties);
        entityManager = entityManagerFactory.createEntityManager();
    }

    public void save(AbstractCatalog catalog) {
        if (!entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
        }
        entityManager.persist(catalog);
    }

    public void save(Locale locale) {
        if (!entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
        }
        entityManager.persist(locale);
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

    public Locale findLocale(String country, String language) {
        return entityManager.createQuery("SELECT o FROM Locale o WHERE o.country = :country AND o.language = :language",
                Locale.class).setParameter("country", country).setParameter("language", language).getSingleResult();
    }

    public <E extends BaseEntity> E findEntity(Class<E> clazz, String id) {
        return entityManager.find(clazz, id);
    }

    public Track findArtistFirstTrack(String id) {
        List<Track> tracks = entityManager.createQuery(
                "SELECT track FROM Track track JOIN track.artists artist JOIN track.albums album JOIN album.names albumName WHERE artist.id = :artistid ORDER BY albumName.value, track.trackNumber",
                Track.class).setParameter("artistid", id).setMaxResults(1).getResultList();
        return tracks.isEmpty() ? null : tracks.get(0);
    }

    public Track findAlbumFirstTrack(String id) {
        List<Track> tracks = entityManager.createQuery(
                "SELECT track FROM Track track JOIN track.albums album WHERE album.id = :albumid ORDER BY track.trackNumber",
                Track.class).setParameter("albumid", id).setMaxResults(1).getResultList();
        return tracks.isEmpty() ? null : tracks.get(0);
    }
}