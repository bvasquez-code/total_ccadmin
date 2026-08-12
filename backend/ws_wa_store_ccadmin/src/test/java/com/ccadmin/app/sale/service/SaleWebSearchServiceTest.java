package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleWebOrderDto;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleWebSearchServiceTest {

    @Mock private SaleDeliveryRepository saleDeliveryRepository;
    @Mock private UserStoreShared userStoreShared;
    @InjectMocks private SaleWebSearchService service;

    @Test
    void filtersWebOrdersByDeliveryTypeAndStatus() {
        when(userStoreShared.getMainStore("SISTEMA")).thenReturn("T001");
        when(saleDeliveryRepository.findWebOrders(
                "pedido",
                "T001",
                SaleConstants.DELIVERY_TYPE_AUTOMATIC,
                SaleConstants.DELIVERY_STATUS_PREPARING,
                10,
                10
        )).thenReturn(List.of());
        when(saleDeliveryRepository.countWebOrders(
                "pedido",
                "T001",
                SaleConstants.DELIVERY_TYPE_AUTOMATIC,
                SaleConstants.DELIVERY_STATUS_PREPARING
        )).thenReturn(0);

        ResponsePageSearchT<SaleWebOrderDto> result = service.findAll(
                " pedido ",
                2,
                SaleConstants.DELIVERY_TYPE_AUTOMATIC,
                SaleConstants.DELIVERY_STATUS_PREPARING
        );

        assertEquals(2, result.Page);
        assertEquals(0, result.TotalResult);
        verify(saleDeliveryRepository).findWebOrders(
                "pedido",
                "T001",
                SaleConstants.DELIVERY_TYPE_AUTOMATIC,
                SaleConstants.DELIVERY_STATUS_PREPARING,
                10,
                10
        );
        verify(saleDeliveryRepository).countWebOrders(
                "pedido",
                "T001",
                SaleConstants.DELIVERY_TYPE_AUTOMATIC,
                SaleConstants.DELIVERY_STATUS_PREPARING
        );
    }

    @Test
    void rejectsUnknownDeliveryStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAll("", 1, "", "INVALID")
        );
    }
}
