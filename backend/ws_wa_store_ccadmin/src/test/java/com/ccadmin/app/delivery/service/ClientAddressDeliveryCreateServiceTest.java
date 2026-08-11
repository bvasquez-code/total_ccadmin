package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAddressDeliveryCreateServiceTest {

    @Mock private ClientDeliveryContextService clientDeliveryContextService;
    @Mock private ClientAddressRepository clientAddressRepository;

    private ClientAddressDeliveryCreateService clientAddressDeliveryCreateService;

    @BeforeEach
    void setUp() {
        clientAddressDeliveryCreateService = new ClientAddressDeliveryCreateService(
                clientDeliveryContextService,
                clientAddressRepository
        );
    }

    @Test
    void createsFirstAddressForAuthenticatedClientAndMakesItDefault() {
        ClientAddressEntity request = validAddress();
        request.ClientAddressID = 999L;
        request.ClientCod = "OTHER";
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
        assertEquals("S", result.IsDefault);
        assertEquals(AuditUserConstants.USER_WEB, result.CreationUser);
    }

    private ClientAddressEntity validAddress() {
        ClientAddressEntity address = new ClientAddressEntity();
        address.Alias = "Casa";
        address.Names = "Cliente Web";
        address.Phone = "999999999";
        address.Address = "Av. Principal 123";
        address.Latitude = new BigDecimal("-6.7812");
        address.Longitude = new BigDecimal("-79.8423");
        address.IsDefault = "N";
        return address;
    }
}
