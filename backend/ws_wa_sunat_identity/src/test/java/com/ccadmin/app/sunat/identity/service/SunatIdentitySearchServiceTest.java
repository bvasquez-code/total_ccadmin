package com.ccadmin.app.sunat.identity.service;

import com.ccadmin.app.sunat.identity.exception.SunatIdentityException;
import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.DniIdentityData;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.sunat.identity.provider.IdentityQueryProvider;
import com.ccadmin.app.sunat.identity.provider.dni.DniIdentityFallbackException;
import com.ccadmin.app.sunat.identity.provider.dni.DniIdentityFallbackProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SunatIdentitySearchServiceTest {

    private final IdentityQueryProvider identityQueryProvider = mock(IdentityQueryProvider.class);
    private final DniIdentityFallbackProvider dniIdentityFallbackProvider =
            mock(DniIdentityFallbackProvider.class);
    private final SunatIdentitySearchService service = new SunatIdentitySearchService(
            this.identityQueryProvider,
            this.dniIdentityFallbackProvider
    );

    @Test
    void validatesAndDelegatesRucQuery() {
        CompanyIdentityResponseDto expected = new CompanyIdentityResponseDto(false, "No encontrado", null);
        when(this.identityQueryProvider.findCompanyByRuc("20123456789")).thenReturn(expected);

        CompanyIdentityResponseDto response = this.service.findCompanyByRuc(" 20123456789 ");

        assertSame(expected, response);
        verify(this.identityQueryProvider).findCompanyByRuc("20123456789");
    }

    @Test
    void translatesDocumentReferenceBeforeDelegating() {
        PersonIdentityResponseDto expected = new PersonIdentityResponseDto(
                false,
                "No encontrado",
                "04",
                "Carnet de extranjería",
                "AB123",
                0,
                List.of(),
                null
        );
        when(this.identityQueryProvider.findPersonByDocument(
                IdentityDocumentType.FOREIGNER_ID,
                "AB123"
        )).thenReturn(expected);

        PersonIdentityResponseDto response = this.service.findPersonByDocument("CE", " ab123 ");

        assertSame(expected, response);
        verify(this.identityQueryProvider).findPersonByDocument(
                IdentityDocumentType.FOREIGNER_ID,
                "AB123"
        );
    }

    @Test
    void rejectsInvalidRucBeforeCallingProvider() {
        assertThrows(IllegalArgumentException.class, () -> this.service.findCompanyByRuc("123"));
    }

    @Test
    void usesDniFallbackWhenSunatDoesNotFindTheDocument() {
        PersonIdentityResponseDto sunatResponse = notFoundDniResponse();
        when(this.identityQueryProvider.findPersonByDocument(
                IdentityDocumentType.DNI,
                "12345678"
        )).thenReturn(sunatResponse);
        when(this.dniIdentityFallbackProvider.findByDni("12345678")).thenReturn(Optional.of(
                new DniIdentityData("12345678", "JUAN CARLOS", "PEREZ", "GOMEZ")
        ));

        PersonIdentityResponseDto response = this.service.findPersonByDocument("01", "12345678");

        assertTrue(response.found());
        assertEquals("01", response.documentTypeCode());
        assertEquals("12345678", response.documentNumber());
        assertEquals(1, response.resultCount());
        assertNull(response.relatedTaxpayers().getFirst().ruc());
        assertEquals(
                "PEREZ GOMEZ JUAN CARLOS",
                response.relatedTaxpayers().getFirst().legalName()
        );
    }

    @Test
    void doesNotUseDniFallbackWhenSunatFindsTheDocument() {
        PersonIdentityResponseDto sunatResponse = new PersonIdentityResponseDto(
                true,
                "Encontrado",
                "01",
                "DNI",
                "12345678",
                1,
                List.of(),
                null
        );
        when(this.identityQueryProvider.findPersonByDocument(
                IdentityDocumentType.DNI,
                "12345678"
        )).thenReturn(sunatResponse);

        PersonIdentityResponseDto response = this.service.findPersonByDocument("01", "12345678");

        assertSame(sunatResponse, response);
        verify(this.dniIdentityFallbackProvider, never()).findByDni("12345678");
    }

    @Test
    void doesNotUseDniFallbackForOtherDocumentTypes() {
        PersonIdentityResponseDto primaryResponse = new PersonIdentityResponseDto(
                false,
                "No encontrado",
                "04",
                "Carnet de extranjería",
                "AB123",
                0,
                List.of(),
                null
        );
        when(this.identityQueryProvider.findPersonByDocument(
                IdentityDocumentType.FOREIGNER_ID,
                "AB123"
        )).thenReturn(primaryResponse);

        PersonIdentityResponseDto response = this.service.findPersonByDocument("04", "AB123");

        assertSame(primaryResponse, response);
        verify(this.dniIdentityFallbackProvider, never()).findByDni("AB123");
    }

    @Test
    void usesDniFallbackWhenSunatIsUnavailable() {
        when(this.identityQueryProvider.findPersonByDocument(
                IdentityDocumentType.DNI,
                "12345678"
        )).thenThrow(new SunatIdentityException("SUNAT no disponible"));
        when(this.dniIdentityFallbackProvider.findByDni("12345678")).thenReturn(Optional.of(
                new DniIdentityData("12345678", "ANA", "TORRES", "DIAZ")
        ));

        PersonIdentityResponseDto response = this.service.findPersonByDocument("01", "12345678");

        assertTrue(response.found());
        assertEquals("TORRES DIAZ ANA", response.relatedTaxpayers().getFirst().legalName());
    }

    @Test
    void preservesSunatNotFoundResponseWhenFallbackIsUnavailable() {
        PersonIdentityResponseDto sunatResponse = notFoundDniResponse();
        when(this.identityQueryProvider.findPersonByDocument(
                IdentityDocumentType.DNI,
                "12345678"
        )).thenReturn(sunatResponse);
        when(this.dniIdentityFallbackProvider.findByDni("12345678"))
                .thenThrow(new DniIdentityFallbackException("Fuente alternativa no disponible"));

        PersonIdentityResponseDto response = this.service.findPersonByDocument("01", "12345678");

        assertSame(sunatResponse, response);
    }

    private PersonIdentityResponseDto notFoundDniResponse() {
        return new PersonIdentityResponseDto(
                false,
                "No encontrado",
                "01",
                "DNI",
                "12345678",
                0,
                List.of(),
                null
        );
    }
}
