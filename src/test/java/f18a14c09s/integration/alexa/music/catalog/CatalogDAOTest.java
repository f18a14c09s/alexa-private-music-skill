package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.catalog.data.*;
import f18a14c09s.integration.hibernate.Hbm2DdlAuto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CatalogDAOTest {
    private EntityManagerFactory entityManagerFactory;
    private CatalogDAO subject;

    @BeforeEach
    void beforeEach() {
        this.entityManagerFactory =
                assertDoesNotThrow(() -> CatalogDAO.defaultEntityManagerFactory(Hbm2DdlAuto.createDrop, null));
        this.subject = new CatalogDAO(entityManagerFactory);
    }

    @Test
    void testFindAllCatalogs() {
        assertNotNull(subject.findAllCatalogs());
        assertEquals(0, subject.findAllCatalogs().size());
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        List<AbstractCatalog> catalogs = Arrays.asList(new MusicAlbumCatalog(),
                new MusicGroupCatalog(),
                new MusicPlaylistCatalog(),
                new MusicRecordingCatalog(),
                new BroadcastChannelCatalog(),
                new GenreCatalog());
        catalogs.forEach(entityManager::persist);
        entityManager.getTransaction().commit();
        catalogs.stream().map(AbstractCatalog::getId).forEach(Assertions::assertNotNull);
        assertEquals(catalogs.size(), catalogs.stream().map(AbstractCatalog::getId).distinct().count());
        List<AbstractCatalog> actualResult = subject.findAllCatalogs();
        assertNotNull(actualResult);
        assertEquals(catalogs.size(), actualResult.size());
        catalogs.forEach(catalog -> assertTrue(actualResult.stream()
                .anyMatch(actualCatalog -> catalog.getClass().equals(actualCatalog.getClass()) &&
                        catalog.getId().equals(actualCatalog.getId()))));
    }

    @Test
    void testSaveLocale() {
        fail("Test not implemented.");
    }

    @Test
    void testSaveArt() {
        fail("Test not implemented.");
    }

    @Test
    void testSaveCatalog() {
        assertNotNull(subject.findAllCatalogs());
        assertEquals(0, subject.findAllCatalogs().size());
        List<AbstractCatalog> catalogs = Arrays.asList(new MusicAlbumCatalog(),
                new MusicGroupCatalog(),
                new MusicPlaylistCatalog(),
                new MusicRecordingCatalog(),
                new BroadcastChannelCatalog(),
                new GenreCatalog());
        catalogs.forEach(subject::save);
        subject.commit();
        catalogs.stream().map(AbstractCatalog::getId).forEach(Assertions::assertNotNull);
        assertEquals(catalogs.size(), catalogs.stream().map(AbstractCatalog::getId).distinct().count());
        List<AbstractCatalog> actualResult = subject.findAllCatalogs();
        assertNotNull(actualResult);
        assertEquals(catalogs.size(), actualResult.size());
        catalogs.forEach(catalog -> assertTrue(actualResult.stream()
                .anyMatch(actualCatalog -> catalog.getClass().equals(actualCatalog.getClass()) &&
                        catalog.getId().equals(actualCatalog.getId()))));
    }

    @Test
    void testFindEntity() {
        fail("Test not implemented.");
    }

    @Test
    void testFindArtistTracks() {
        fail("Test not implemented.");
    }

    @Test
    void testFindAlbumTracks() {
        fail("Test not implemented.");
    }

    @Test
    void testCommit() {
        fail("Test not implemented.");
    }

    @Test
    void testClose() {
        fail("Test not implemented.");
    }

    @Test
    void testCount() {
        fail("Test not implemented.");
    }

    @Test
    void testFindAllArtistReferences() {
        fail("Test not implemented.");
    }

    @Test
    void testFindAllAlbumReferences() {
        fail("Test not implemented.");
    }

    @Test
    void testGetCataloguedEntityIdsByTypeAndNaturalKey() {
        fail("Test not implemented.");
    }

    @Test
    void testFindLocale() {
        fail("Test not implemented.");
    }
}
