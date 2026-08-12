package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.ccadmin.app.shared.model.idto.IAddressLocationDto;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAddressDeliveryCreateServiceTest {

    @Mock private ClientDeliveryContextService clientDeliveryContextService;
    @Mock private ClientAddressRepository clientAddressRepository;
    @Mock private UbigeoRepository ubigeoRepository;
    @Mock private LocationRepository locationRepository;

    private ClientAddressDeliveryCreateService clientAddressDeliveryCreateService;

    @BeforeEach
    void setUp() {
        clientAddressDeliveryCreateService = new ClientAddressDeliveryCreateService(
                clientDeliveryContextService,
                clientAddressRepository,
                ubigeoRepository,
                locationRepository
        );
    }

    @Test
    void createsFirstAddressForAuthenticatedClientAndMakesItDefault() {
        ClientAddressEntity request = validAddress();
        request.ClientAddressID = null;
        request.ClientCod = "OTHER";
        when(ubigeoRepository.findPeruLocation("140101")).thenReturn(Optional.of(
                location("PER", "Perú", "Lambayeque", "Chiclayo")
        ));
        when(clientDeliveryContextService.getCurrentClient()).thenReturn(
                new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente")
        );
        when(clientAddressRepository.findActiveByClientCod("CL001")).thenReturn(List.of());
        when(clientAddressRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientAddressEntity result = clientAddressDeliveryCreateService.save(request);

        ArgumentCaptor<ClientAddressEntity> captor = ArgumentCaptor.forClass(ClientAddressEntity.class);
        verify(clientAddressRepository).save(captor.capture());
        verify(clientAddressRepository).clearDefaultByClientCod(
                "CL001",
                AuditUserConstants.USER_WEB
        );
        assertNull(captor.getValue().ClientAddressID);
        assertEquals("CL001", result.ClientCod);
        assertEquals("PER", result.CountryCod);
        assertEquals("Perú", result.CountryName);
        assertEquals("Lambayeque", result.StateName);
        assertEquals("Chiclayo", result.CityName);
        assertEquals("S", result.IsDefault);
        assertEquals(AuditUserConstants.USER_WEB, result.CreationUser);
    }

    @Test
    void updatesOnlyAddressOwnedByAuthenticatedClient() {
        ClientAddressEntity request = validAddress();
        request.ClientAddressID = 22L;
        request.Alias = "Oficina actualizada";
        ClientAddressEntity current = validAddress();
        current.ClientAddressID = 22L;
        current.ClientCod = "CL001";
        current.CreationUser = "USER_WEB";
        current.IsDefault = "N";

        when(ubigeoRepository.findPeruLocation("140101")).thenReturn(Optional.of(
                location("PER", "Perú", "Lambayeque", "Chiclayo")
        ));

        when(clientDeliveryContextService.getCurrentClient()).thenReturn(
                new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente")
        );
        when(clientAddressRepository.findActiveByClientCod("CL001")).thenReturn(List.of(current));
        when(clientAddressRepository.findActiveByClientAddressIdAndClientCod(22L, "CL001"))
                .thenReturn(java.util.Optional.of(current));
        when(clientAddressRepository.save(current)).thenReturn(current);

        ClientAddressEntity result = clientAddressDeliveryCreateService.save(request);

        assertSame(current, result);
        assertEquals(22L, result.ClientAddressID);
        assertEquals("CL001", result.ClientCod);
        assertEquals("Oficina actualizada", result.Alias);
        assertEquals("USER_WEB", result.CreationUser);
        assertEquals(AuditUserConstants.USER_WEB, result.ModifyUser);
        verify(clientAddressRepository, never()).clearDefaultByClientCod(
                "CL001",
                AuditUserConstants.USER_WEB
        );
    }

    @Test
    void rejectsAddressWhenDistrictDoesNotExist() {
        ClientAddressEntity request = validAddress();
        request.UbigeoCod = "999999";
        when(ubigeoRepository.findPeruLocation("999999")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> clientAddressDeliveryCreateService.save(request)
        );

        assertEquals("Selecciona un departamento, provincia y distrito válidos", exception.getMessage());
        verify(clientAddressRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAddressWithoutUbigeo() {
        ClientAddressEntity request = validAddress();
        request.UbigeoCod = "";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> clientAddressDeliveryCreateService.save(request)
        );

        assertEquals("El ubigeo es obligatorio", exception.getMessage());
        verify(ubigeoRepository, never()).findPeruLocation(org.mockito.ArgumentMatchers.anyString());
        verify(clientAddressRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsForeignAddressUsingCanonicalLocationNames() {
        ClientAddressEntity request = validAddress();
        request.CountryCod = "USA";
        request.CountryName = "Texto alterado";
        request.StateId = 10L;
        request.CityId = 20L;
        request.UbigeoCod = "90210";
        when(locationRepository.findForeignLocation("USA", 10L, 20L)).thenReturn(Optional.of(
                location("USA", "Estados Unidos", "California", "Beverly Hills")
        ));
        when(clientDeliveryContextService.getCurrentClient()).thenReturn(
                new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente")
        );
        when(clientAddressRepository.findActiveByClientCod("CL001")).thenReturn(List.of());
        when(clientAddressRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientAddressEntity result = clientAddressDeliveryCreateService.save(request);

        assertEquals("USA", result.CountryCod);
        assertEquals("Estados Unidos", result.CountryName);
        assertEquals("California", result.StateName);
        assertEquals("Beverly Hills", result.CityName);
        assertEquals("90210", result.UbigeoCod);
        verify(ubigeoRepository, never()).findPeruLocation(org.mockito.ArgumentMatchers.anyString());
    }

    private ClientAddressEntity validAddress() {
        ClientAddressEntity address = new ClientAddressEntity();
        address.Alias = "Casa";
        address.Names = "Cliente Web";
        address.Phone = "999999999";
        address.Address = "Av. Principal 123";
        address.CountryCod = "PER";
        address.UbigeoCod = "140101";
        address.Latitude = new BigDecimal("-6.7812");
        address.Longitude = new BigDecimal("-79.8423");
        address.IsDefault = "N";
        return address;
    }

    private IAddressLocationDto location(
            String countryCod,
            String countryName,
            String stateName,
            String cityName
    ) {
        return new IAddressLocationDto() {
            public String getCountryCod() { return countryCod; }
            public String getCountryName() { return countryName; }
            public String getStateName() { return stateName; }
            public String getCityName() { return cityName; }
        };
    }
}
