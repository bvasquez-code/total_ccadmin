package com.ccadmin.app.sunat.identity.provider.sunat;

import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunatPersonDocumentHtmlParserTest {

    private final SunatPersonDocumentHtmlParser parser = new SunatPersonDocumentHtmlParser();

    @Test
    void mapsAllTaxpayersRelatedToPersonDocument() throws IOException {
        PersonIdentityResponseDto response = this.parser.parse(
                IdentityDocumentType.DNI,
                "12345678",
                resource("/sunat/person-document-found.html")
        );

        assertTrue(response.found());
        assertEquals("01", response.documentTypeCode());
        assertEquals("DNI", response.documentTypeName());
        assertEquals(2, response.resultCount());
        assertEquals("EMPRESA RELACIONADA S.A.C.", response.relatedTaxpayers().get(1).legalName());
        assertEquals("AREQUIPA", response.relatedTaxpayers().get(1).location());
        assertEquals("04/08/2026 10:20", response.queryDate());
    }

    @Test
    void returnsEmptyResultWhenSunatHasNoRelatedTaxpayers() throws IOException {
        PersonIdentityResponseDto response = this.parser.parse(
                IdentityDocumentType.FOREIGNER_ID,
                "ABC123",
                resource("/sunat/not-found.html")
        );

        assertFalse(response.found());
        assertEquals(0, response.resultCount());
        assertTrue(response.relatedTaxpayers().isEmpty());
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
