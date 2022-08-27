package f18a14c09s.integration.alexa.smapi;

import f18a14c09s.integration.alexa.smapi.data.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class CatalogContentUploadCLI {
        private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

        static {
                JSON_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        }

        public static Map<String, List<Catalog>> listEmptyCatalogsByType() throws IOException {
                CatalogContentUploadClient client = new CatalogContentUploadClient(
                        "Atza|IwEBIEib9cCEBZNGRgw0GfIUqf9PHZkhu0z56YgodkS_QH1iU4f2TLnBOUxpNFI8gPrKwd78oSuB9RwEbBVlNGkxxt0LciOTRzZGKeLUxMqqDfmiYYzNGhLH-jnmVT1Jbw1WK1sP6HcDNcP5ZzUAMuYTEThRBEGZ17ylzH9_LSN_k_5VHFbc4Ou7rC5oTeI8z3clYGqVStENvCpOh04GlNX6EtRZe4mNh8j-NMve26saAzR66uWlv20Kl-9k95qIuCV_uLkg7rSeYampqyVkUXPpNP7bJ6xobLLzK-uImiCX5LkxeavUSziuNHnggnI6W80UNQd242TAJdYgmZpucvPJXS-hQ4Of7HZnce6HbxsS8aDcP7eF2alz_fM8-5aM6K1R7YEuWEidsNa-JwpEl28mTtFuX7haP19A19-iqztCp6F4ft-N3vhR5Y-L9O5HcvukoZqH8S8QSBAwT6mNeU9gH303aVQC1tcpQk6N9GKr9jyQhVxJn8COY7Ro7Bek3o1--DZ39sOaZJ4oedlfrOnavtUt2TlWbC6Tx2znuOgtFYrfpdG2RJ4drkQIhsKOxIlG_8I0rDdDTzkLBFEgJk-aEmrya7ofXpBtJnKox6n0iwklITY5x_ao1TLPMHexEBxF2PWfCCjn6FFPN-9cHqFaRNspALF6pZLOtip4ToiMEloQEg"
                );
                String vendorId = "M38T1FI3J5ZKZ";
                List<Catalog> catalogs = client.listCatalogs(vendorId);
                Map<String, List<Catalog>> emptyCatalogsByType = new HashMap<>();
                for (Catalog catalog : catalogs) {
                        List<Upload> uploads = client.listUploads(catalog.getId());
                        if (uploads.isEmpty()) {
                                emptyCatalogsByType
                                                .computeIfAbsent(catalog.getType(),
                                                                key -> new ArrayList<>())
                                                .add(catalog);
                        }
                }
                return emptyCatalogsByType;
        }

        public static void main(String... args) throws IOException {
                System.out.println(JSON_MAPPER.writeValueAsString(listEmptyCatalogsByType()));
        }
}
