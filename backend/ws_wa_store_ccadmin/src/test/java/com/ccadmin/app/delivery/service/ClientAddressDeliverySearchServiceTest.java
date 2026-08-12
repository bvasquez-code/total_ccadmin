package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAddressDeliverySearchServiceTest {

    @Mock private ClientDeliveryContextService clientDeliveryContextService;
    @Mock private ClientAddressRepository clientAddressRepository;
    @Mock private UbigeoRepository ubigeoRepository;
    @Mock private LocationRepository locationRepository;

    private ClientAddressDeliverySearchService clientAddressDeliverySearchService;

    @BeforeEach
    void setUp() {
        clientAddressDeliverySearchService = new ClientAddressDeliverySearchService(
                clientDeliveryContextService,
                clientAddressRepository,
                ubigeoRepository,
                locationRepository
        );
    }

    @Test
    void findsOnlyAddressesForTheAuthenticatedClient() {
        ClientSessionDto clientSession = new ClientSessionDto(
                1L,
                "77889966",
                "client@example.com",
                "Cliente Web"
        );
        List<ClientAddressEntity> expectedAddressList = List.of(new ClientAddressEntity());
        when(clientDeliveryContextService.getCurrentClient()).thenReturn(clientSession);
        when(clientAddressRepository.findActiveByClientCod("77889966"))
                .thenReturn(expectedAddressList);

        List<ClientAddressEntity> result =
                clientAddressDeliverySearchService.findAllForCurrentClient();

        assertSame(expectedAddressList, result);
        verify(clientAddressRepository).findActiveByClientCod("77889966");
    }
}
