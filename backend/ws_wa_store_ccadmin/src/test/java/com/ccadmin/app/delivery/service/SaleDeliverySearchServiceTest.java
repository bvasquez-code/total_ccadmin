package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.SaleDeliveryOrderDto;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.idto.ISaleDeliveryOrderDto;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.service.SaleSearchService;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleDeliverySearchServiceTest {

    @Mock private ClientDeliveryContextService clientDeliveryContextService;
    @Mock private SaleHeadRepository saleHeadRepository;
    @Mock private SaleSearchService saleSearchService;
    @Mock private SaleDeliveryAccessTokenService saleDeliveryAccessTokenService;
    @InjectMocks private SaleDeliverySearchService service;

    @Test
    void listsAuthenticatedClientOrdersAndIssuesTokenOnlyForPendingPayment() {
        ClientSessionDto client = new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente");
        ISaleDeliveryOrderDto pendingOrder = order("ST001", SaleConstants.PENDING, "N", 0L);
        ISaleDeliveryOrderDto paidOrder = order("ST002", SaleConstants.PENDING, "S", 1L);

        when(clientDeliveryContextService.getCurrentClient()).thenReturn(client);
        when(saleHeadRepository.countWebSalesByClientCod("CL001")).thenReturn(2);
        when(saleHeadRepository.findWebSalesByClientCod("CL001", 0, 10))
                .thenReturn(List.of(pendingOrder, paidOrder));
        when(saleDeliveryAccessTokenService.issue("ST001", "CL001"))
                .thenReturn("v1.pending-order");

        ResponsePageSearchT<SaleDeliveryOrderDto> result = service.findMyOrders(1);

        assertEquals(2, result.TotalResult);
        assertEquals(1, result.TotalPages);
        assertEquals(2, result.resultSearch.size());
        assertTrue(result.resultSearch.get(0).CanResumePayment);
        assertEquals("v1.pending-order", result.resultSearch.get(0).OrderToken);
        assertFalse(result.resultSearch.get(1).CanResumePayment);
        verify(saleDeliveryAccessTokenService, never()).issue("ST002", "CL001");
    }

    private ISaleDeliveryOrderDto order(
            String saleCod,
            String saleStatus,
            String isPaid,
            long paymentCount
    ) {
        ISaleDeliveryOrderDto order = org.mockito.Mockito.mock(ISaleDeliveryOrderDto.class);
        when(order.getSaleCod()).thenReturn(saleCod);
        when(order.getNumTotalPrice()).thenReturn(new BigDecimal("100.00"));
        when(order.getNumTotalPaid()).thenReturn(
                "S".equals(isPaid) ? new BigDecimal("100.00") : BigDecimal.ZERO
        );
        when(order.getPaymentCount()).thenReturn(paymentCount);
        when(order.getCurrencyCod()).thenReturn("PEN");
        when(order.getSaleStatus()).thenReturn(saleStatus);
        when(order.getIsPaid()).thenReturn(isPaid);
        when(order.getDeliveryStatus()).thenReturn(SaleConstants.DELIVERY_STATUS_PENDING);
        return order;
    }
}
