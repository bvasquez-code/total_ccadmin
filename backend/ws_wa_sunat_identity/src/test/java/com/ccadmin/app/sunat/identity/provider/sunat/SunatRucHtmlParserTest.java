package com.ccadmin.app.sunat.identity.provider.sunat;

import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunatRucHtmlParserTest {

    private final SunatRucHtmlParser parser = new SunatRucHtmlParser();

    @Test
    void mapsRucHtmlToEnglishResponseFields() throws IOException {
        CompanyIdentityResponseDto response = this.parser.parse(
                "20123456789",
                resource("/sunat/ruc-found.html")
        );

        assertTrue(response.found());
        assertEquals("20123456789", response.company().ruc());
        assertEquals("EMPRESA EJEMPLO S.A.C.", response.company().legalName());
        assertEquals("ACTIVO", response.company().taxpayerStatus());
        assertEquals(2, response.company().economicActivities().size());
        assertEquals(3, response.company().electronicReceipts().size());
        assertEquals("04/08/2026 10:15", response.company().queryDate());
    }

    @Test
    void returnsNotFoundWithoutFabricatingCompany() throws IOException {
        CompanyIdentityResponseDto response = this.parser.parse(
                "20123456789",
                resource("/sunat/not-found.html")
        );

        assertFalse(response.found());
        assertNull(response.company());
        assertEquals("No se encontraron resultados para la búsqueda realizada.", response.message());
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
