package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.sale.model.entity.VirtualCartEntity;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.sale.repository.VirtualCartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleDeliveryCreateServiceTest {

    @Mock
    private VirtualCartRepository virtualCartRepository;
    @Mock
    private SaleDeliveryRepository saleDeliveryRepository;

    @Test
    void convertsCartDeliverySnapshotWhenSaleIsCreated() {
        VirtualCartEntity cart = new VirtualCartEntity();
        cart.PresaleCod = "PS001";
        cart.CartData = """
                {"Delivery":{"DeliveryTypeCod":"DELIVERY","Names":"Cliente",\
                "Phone":"999999999","Address":"Calle 1","Latitude":-6.77,"Longitude":-79.84}}
                """;
        when(virtualCartRepository.findConvertedByPresaleCod("PS001"))
                .thenReturn(Optional.of(cart));
        SaleDeliveryCreateService service = new SaleDeliveryCreateService(
                virtualCartRepository,
                saleDeliveryRepository,
                new ObjectMapper()
        );

        service.createFromConvertedCart("PS001", "ST001", "USER01");

        ArgumentCaptor<SaleDeliveryEntity> deliveryCaptor =
                ArgumentCaptor.forClass(SaleDeliveryEntity.class);
        verify(saleDeliveryRepository).save(deliveryCaptor.capture());
        assertEquals("ST001", deliveryCaptor.getValue().SaleCod);
        assertEquals(SaleConstants.DELIVERY_STATUS_PENDING, deliveryCaptor.getValue().DeliveryStatus);
        assertEquals("DELIVERY", deliveryCaptor.getValue().DeliveryTypeCod);
        assertEquals("ST001", cart.SaleCod);
        verify(virtualCartRepository).save(cart);
    }
}
