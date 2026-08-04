package com.ccadmin.app.sunat.identity.model.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityDocumentTypeTest {

    @Test
    void resolvesCanonicalCodesAndAliases() {
        assertEquals(IdentityDocumentType.DNI, IdentityDocumentType.fromReference("01"));
        assertEquals(IdentityDocumentType.FOREIGNER_ID, IdentityDocumentType.fromReference("CE"));
        assertEquals(IdentityDocumentType.PASSPORT, IdentityDocumentType.fromReference("passport"));
        assertEquals(IdentityDocumentType.DIPLOMATIC_ID, IdentityDocumentType.fromReference("A"));
    }

    @Test
    void validatesAndNormalizesDocumentNumbers() {
        assertEquals("12345678", IdentityDocumentType.DNI.normalizeDocumentNumber(" 12345678 "));
        assertEquals("AB-123", IdentityDocumentType.PASSPORT.normalizeDocumentNumber("ab-123"));

        assertThrows(
                IllegalArgumentException.class,
                () -> IdentityDocumentType.DNI.normalizeDocumentNumber("ABC12345")
        );
    }

    @Test
    void rejectsUnsupportedDocumentType() {
        assertThrows(IllegalArgumentException.class, () -> IdentityDocumentType.fromReference("06"));
    }
}
