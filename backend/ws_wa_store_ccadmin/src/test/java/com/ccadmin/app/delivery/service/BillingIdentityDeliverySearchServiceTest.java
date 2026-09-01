package com.ccadmin.app.delivery.service;

import com.ccadmin.app.identity.model.dto.CompanyIdentityDto;
import com.ccadmin.app.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.identity.service.SunatIdentitySearchService;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingIdentityDeliverySearchServiceTest {

    @Mock private PersonShared personShared;
    @Mock private SunatIdentitySearchService sunatIdentitySearchService;

    private BillingIdentityDeliverySearchService service;

    @BeforeEach
    void setUp() {
        service = new BillingIdentityDeliverySearchService(
                personShared,
                sunatIdentitySearchService
        );
    }

    @Test
    void returnsCompanyFoundBySunatIdentityWithoutSearchingInternally() {
        CompanyIdentityDto company = company(
                "EMPRESA DE PRUEBA S.A.C.",
                "PRUEBA",
                "AV. LIMA 123"
        );
        when(sunatIdentitySearchService.findCompanyByRuc("20100017491"))
                .thenReturn(new CompanyIdentityResponseDto(true, "Encontrado", company));

        PersonEntity result = service.findCompanyByRuc("20100017491");

        assertEquals("04", result.PersonType);
        assertEquals("06", result.DocumentType);
        assertEquals("20100017491", result.DocumentNum);
        assertEquals("EMPRESA DE PRUEBA S.A.C.", result.BusinessName);
        assertEquals("PRUEBA", result.CommercialName);
        assertEquals("AV. LIMA 123", result.Address);
        verify(personShared, never()).findByDocumentNum("06", "20100017491");
    }

    @Test
    void searchesInternallyWhenSunatDoesNotFindTheCompany() {
        PersonEntity expected = new PersonEntity();
        expected.DocumentNum = "20100017491";
        when(sunatIdentitySearchService.findCompanyByRuc(expected.DocumentNum))
                .thenReturn(new CompanyIdentityResponseDto(false, "No encontrado", null));
        when(personShared.findByDocumentNum("06", expected.DocumentNum)).thenReturn(expected);

        PersonEntity result = service.findCompanyByRuc(expected.DocumentNum);

        assertEquals(expected, result);
        verify(personShared).findByDocumentNum("06", expected.DocumentNum);
    }

    @Test
    void searchesInternallyWhenSunatIdentityIsUnavailable() {
        when(sunatIdentitySearchService.findCompanyByRuc("20100017491"))
                .thenThrow(new IllegalStateException("No disponible"));
        when(personShared.findByDocumentNum("06", "20100017491")).thenReturn(null);

        PersonEntity result = service.findCompanyByRuc("20100017491");

        assertNull(result);
        verify(personShared).findByDocumentNum("06", "20100017491");
    }

    @Test
    void rejectsAnInvalidRucBeforeSearching() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findCompanyByRuc("123")
        );

        assertEquals("El RUC debe contener 11 digitos", exception.getMessage());
        verify(sunatIdentitySearchService, never()).findCompanyByRuc("123");
        verify(personShared, never()).findByDocumentNum("06", "123");
    }

    private CompanyIdentityDto company(
            String legalName,
            String tradeName,
            String fiscalAddress
    ) {
        return new CompanyIdentityDto(
                "20100017491",
                legalName,
                null,
                tradeName,
                null,
                null,
                null,
                null,
                fiscalAddress,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                null
        );
    }
}
