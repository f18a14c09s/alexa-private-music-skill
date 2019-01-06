package f18a14c09s.integration.alexa.music.catalog;

import f18a14c09s.integration.alexa.music.catalog.data.AbstractCatalog;
import f18a14c09s.integration.json.JSONAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CatalogDbToDiskCLI {
    private JSONAdapter jsonAdapter;
    private CatalogDAO dao;
    private File destDir;

    public CatalogDbToDiskCLI() throws IOException {
        this.jsonAdapter = new JSONAdapter();
        this.dao = new CatalogDAO();
        this.destDir = new File("C:\\Users\\fjohnson\\Music\\Private Music Skill");
    }

    public static void main(String... args) throws IOException {
        new CatalogDbToDiskCLI().writeAllCatalogsToDisk();
    }

    private void writeAllCatalogsToDisk() throws IOException {
        for (AbstractCatalog catalog : dao.findAllCatalogs()) {
            writeToDisk(catalog,
                    new File(destDir, String.format("%s Catalog %s.json", catalog.getType(), catalog.getId())));
        }
    }

    private void writeToDisk(AbstractCatalog catalog, File destFile) throws IOException {
        System.out.printf("Writing to %s.%n", destFile.getAbsolutePath());
        try (FileWriter fw = new FileWriter(destFile)) {
            jsonAdapter.writeValue(fw, catalog);
        }
    }
}
