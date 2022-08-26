package f18a14c09s.integration.alexa.smapi;

import f18a14c09s.integration.alexa.smapi.data.Catalog;

import java.io.IOException;
import java.util.List;

public class CatalogContentUploadCLI {
    public static void main(String... args) throws IOException {
        CatalogContentUploadClient client = new CatalogContentUploadClient(
                ""
        );
        String vendorId = "";
        List<Catalog> catalogs = client.listCatalogs(
                vendorId
        );
//        catalogs = catalogs.stream().filter(
//                catalog -> catalog.getAssociatedSkillIds() != null
//                        && !catalog.getAssociatedSkillIds().isEmpty()
//        ).collect(Collectors.toList());
//        System.out.println(
//                client.jsonMapper.writeValueAsString(
//                        catalogs
//                )
//        );
//        for (Catalog catalog : catalogs) {
//            List<Upload> uploads = client.listUploads(
//                    catalog.getId()
//            );
//            uploads = uploads.stream().filter(
//                    upload -> upload.getStatus() != null && upload.getStatus().equals("SUCCEEDED")
//            ).map(
//                    upload ->
//                    {
//                        try {
//                            return client.getUpload(
//                                    upload.getCatalogId(),
//                                    upload.getId()
//                            );
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//            ).collect(Collectors.toList());
//            System.out.println(
//                    client.jsonMapper.writeValueAsString(
//                            uploads
//                    )
//            );
//        }
//        MusicRecordingCatalog trackCatalog = new MusicRecordingCatalog();
//        System.out.println(client.createCatalog(
//                trackCatalog,
//                ""
//        ));
    }
}
