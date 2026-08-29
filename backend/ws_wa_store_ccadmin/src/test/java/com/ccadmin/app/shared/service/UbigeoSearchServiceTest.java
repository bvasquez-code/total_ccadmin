package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.model.idto.ILocationOptionDto;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UbigeoSearchServiceTest {

    @Mock private UbigeoRepository ubigeoRepository;
    @Mock private LocationRepository locationRepository;

    private UbigeoSearchService ubigeoSearchService;

    @BeforeEach
    void setUp() {
        ubigeoSearchService = new UbigeoSearchService(ubigeoRepository, locationRepository);
    }

    @Test
    void mapsCountriesFromTheSharedLocationRepository() {
        ILocationOptionDto country = locationOption("PER", "Peru");
        when(locationRepository.findCountries()).thenReturn(List.of(country));

        var result = ubigeoSearchService.findCountries();

        assertEquals(1, result.size());
        assertEquals("PER", result.get(0).Code);
        assertEquals("Peru", result.get(0).Name);
    }

    @Test
    void delegatesValidProvinceSearch() {
        when(ubigeoRepository.findProvinces("14")).thenReturn(List.of());

        ubigeoSearchService.findProvinces("14");

        verify(ubigeoRepository).findProvinces("14");
    }

    @Test
    void rejectsInvalidProvinceCode() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ubigeoSearchService.findDistricts("140")
        );

        assertEquals("El codigo de provincia debe tener 4 digitos", exception.getMessage());
    }

    private ILocationOptionDto locationOption(String code, String name) {
        ILocationOptionDto locationOption = mock(ILocationOptionDto.class);
        when(locationOption.getCode()).thenReturn(code);
        when(locationOption.getName()).thenReturn(name);
        return locationOption;
    }
}
