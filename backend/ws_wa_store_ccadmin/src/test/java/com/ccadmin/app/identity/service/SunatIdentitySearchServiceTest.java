package com.ccadmin.app.identity.service;

import com.ccadmin.app.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SunatIdentitySearchServiceTest {

    @Mock private BusinessConfigSearchService businessConfigSearchService;
    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private SunatIdentitySearchService service;

    @BeforeEach
    void setUp() {
        service = new SunatIdentitySearchService(
                businessConfigSearchService,
                new ObjectMapper(),
                httpClient
        );
    }

    @Test
    void findsCompanyUsingItsConfiguredUrl() throws Exception {
        when(businessConfigSearchService.findByConfigCod(
                SunatIdentitySearchService.CONFIG_GROUP,
                SunatIdentitySearchService.FIND_COMPANY_BY_RUC_CONFIG
        )).thenReturn(activeConfiguration(
                SunatIdentitySearchService.FIND_COMPANY_BY_RUC_CONFIG,
                "http://localhost:8093/api/v1/sunatIdentity/findCompanyByRuc"
        ));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                {
                  "Status": "200",
                  "ErrorStatus": false,
                  "Data": {
                    "found": true,
                    "message": "Encontrado",
                    "company": {
                      "ruc": "20100017491",
                      "legalName": "EMPRESA DE PRUEBA S.A.C.",
                      "tradeName": "PRUEBA",
                      "fiscalAddress": "AV. LIMA 123"
                    }
                  }
                }
                """);
        mockHttpResponse();

        CompanyIdentityResponseDto result = service.findCompanyByRuc("20100017491");

        assertTrue(result.found());
        assertEquals("EMPRESA DE PRUEBA S.A.C.", result.company().legalName());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertEquals("Ruc=20100017491", requestCaptor.getValue().uri().getQuery());
    }

    @Test
    void findsPersonUsingTheDedicatedConfiguredUrl() throws Exception {
        when(businessConfigSearchService.findByConfigCod(
                SunatIdentitySearchService.CONFIG_GROUP,
                SunatIdentitySearchService.FIND_PERSON_BY_DOCUMENT_CONFIG
        )).thenReturn(activeConfiguration(
                SunatIdentitySearchService.FIND_PERSON_BY_DOCUMENT_CONFIG,
                "http://localhost:8093/api/v1/sunatIdentity/findPersonByDocument"
        ));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                {
                  "ErrorStatus": false,
                  "Data": {
                    "found": true,
                    "message": "Encontrado",
                    "documentTypeCode": "01",
                    "documentTypeName": "DNI",
                    "documentNumber": "12345678",
                    "resultCount": 1,
                    "relatedTaxpayers": [{"legalName": "PEREZ DIAZ JUAN"}],
                    "queryDate": "01/09/2026 12:00"
                  }
                }
                """);
        mockHttpResponse();

        PersonIdentityResponseDto result = service.findPersonByDocument("01", "12345678");

        assertTrue(result.found());
        assertEquals("PEREZ DIAZ JUAN", result.relatedTaxpayers().get(0).legalName());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertEquals(
                "DocumentType=01&DocumentNumber=12345678",
                requestCaptor.getValue().uri().getQuery()
        );
    }

    @Test
    void rejectsInactiveConfigurationBeforeCallingTheProvider() throws Exception {
        BusinessConfigEntity configuration = activeConfiguration(
                SunatIdentitySearchService.FIND_COMPANY_BY_RUC_CONFIG,
                "http://localhost:8093/api/v1/sunatIdentity/findCompanyByRuc"
        );
        configuration.Status = "I";
        when(businessConfigSearchService.findByConfigCod(
                SunatIdentitySearchService.CONFIG_GROUP,
                SunatIdentitySearchService.FIND_COMPANY_BY_RUC_CONFIG
        )).thenReturn(configuration);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findCompanyByRuc("20100017491")
        );

        assertTrue(exception.getMessage().contains("no esta configurada o esta inactiva"));
        verify(httpClient, never()).send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
    }

    private void mockHttpResponse() throws Exception {
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);
    }

    private BusinessConfigEntity activeConfiguration(String code, String url) {
        BusinessConfigEntity configuration = new BusinessConfigEntity();
        configuration.GroupCod = SunatIdentitySearchService.CONFIG_GROUP;
        configuration.ConfigCod = code;
        configuration.ConfigVal = url;
        configuration.Status = "A";
        return configuration;
    }
}
