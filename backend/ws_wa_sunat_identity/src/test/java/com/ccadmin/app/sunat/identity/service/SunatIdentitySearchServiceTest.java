package com.ccadmin.app.sunat.identity.service;

import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.sunat.identity.provider.IdentityQueryProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SunatIdentitySearchServiceTest {

    private final IdentityQueryProvider identityQueryProvider = mock(IdentityQueryProvider.class);
    private final SunatIdentitySearchService service = new SunatIdentitySearchService(
            this.identityQueryProvider
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
}
