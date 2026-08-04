package com.ccadmin.app.sunat.identity.provider.dni.eldni;

import com.ccadmin.app.sunat.identity.model.dto.DniIdentityData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElDniHtmlParserTest {

    private final ElDniHtmlParser parser = new ElDniHtmlParser();

    @Test
    void mapsNamesAndSurnamesForRequestedDni() throws IOException {
        Optional<DniIdentityData> result = this.parser.parse(
                "12345678",
                resource("/eldni/dni-found.html")
        );

        assertTrue(result.isPresent());
        assertEquals("12345678", result.get().documentNumber());
        assertEquals("JUAN CARLOS", result.get().names());
        assertEquals("PEREZ", result.get().paternalSurname());
        assertEquals("GOMEZ", result.get().maternalSurname());
    }

    @Test
    void ignoresRowsBelongingToAnotherDni() throws IOException {
        Optional<DniIdentityData> result = this.parser.parse(
                "87654321",
                resource("/eldni/dni-found.html")
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyResultWhenResponseHasNoDataTable() {
        Optional<DniIdentityData> result = this.parser.parse(
                "12345678",
                "<html><body><div class='alert'>No encontrado</div></body></html>"
        );

        assertTrue(result.isEmpty());
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("No se encontró el fixture " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
