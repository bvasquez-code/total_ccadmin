package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.AddressGeocodingResultDto;
import com.ccadmin.app.shared.model.entity.CountryEntity;
import com.ccadmin.app.shared.repository.LocationRepository;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressGeocodingSearchServiceTest {

    @Mock private LocationRepository locationRepository;
    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private AddressGeocodingSearchService addressGeocodingSearchService;

    @BeforeEach
    void setUp() {
        addressGeocodingSearchService = new AddressGeocodingSearchService(
                new ObjectMapper(),
                locationRepository,
                "https://nominatim.openstreetmap.org/search",
                "CcAdmin-Test/1.0",
                httpClient
        );
    }

    @Test
    void searchesByCountryAndCachesTheProviderResult() throws Exception {
        CountryEntity peru = new CountryEntity();
        peru.CountryCod = "PER";
        peru.CountryIso2 = "PE";
        when(locationRepository.findActiveCountryByCode("PER")).thenReturn(Optional.of(peru));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                [{
                  "display_name":"Avenida José Balta 895, Chiclayo, Perú",
                  "lat":"-6.77140000",
                  "lon":"-79.84090000",
                  "address":{"postcode":"14001"}
                }]
                """);
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        List<AddressGeocodingResultDto> firstResult = addressGeocodingSearchService.search(
                "  Avenida José Balta 895   Chiclayo ",
                "per"
        );
        List<AddressGeocodingResultDto> cachedResult = addressGeocodingSearchService.search(
                "Avenida José Balta 895 Chiclayo",
                "PER"
        );

        assertEquals(1, firstResult.size());
        assertEquals("Avenida José Balta 895, Chiclayo, Perú", firstResult.get(0).DisplayName);
        assertEquals("14001", firstResult.get(0).PostalCode);
        assertEquals(new BigDecimal("-6.77140000"), firstResult.get(0).Latitude);
        assertEquals(new BigDecimal("-79.84090000"), firstResult.get(0).Longitude);
        assertEquals(firstResult, cachedResult);
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertTrue(requestCaptor.getValue().uri().getQuery().contains("countrycodes=pe"));
        assertEquals(
                "CcAdmin-Test/1.0",
                requestCaptor.getValue().headers().firstValue("User-Agent").orElse("")
        );
    }

    @Test
    void rejectsCountryWithoutIso2BeforeCallingProvider() throws Exception {
        CountryEntity country = new CountryEntity();
        country.CountryCod = "XXX";
        when(locationRepository.findActiveCountryByCode("XXX")).thenReturn(Optional.of(country));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> addressGeocodingSearchService.search("Dirección válida", "XXX")
        );

        assertEquals(
                "El país seleccionado no tiene configurado su código ISO2",
                exception.getMessage()
        );
        verify(httpClient, never()).send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
    }
}
