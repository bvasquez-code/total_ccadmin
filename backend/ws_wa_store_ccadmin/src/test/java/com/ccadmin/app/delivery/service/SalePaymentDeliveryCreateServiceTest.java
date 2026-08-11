package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.SalePaymentDeliveryRegisterDto;
import com.ccadmin.app.delivery.model.dto.SaleDeliveryAccessTokenPayloadDto;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.service.TrxPaymentCreateService;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.sale.service.SalePaymentCreateService;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalePaymentDeliveryCreateServiceTest {

    @Mock private ClientDeliveryContextService clientDeliveryContextService;
    @Mock private SaleHeadRepository saleHeadRepository;
    @Mock private SalePaymentRepository salePaymentRepository;
    @Mock private TrxPaymentCreateService trxPaymentCreateService;
    @Mock private SalePaymentCreateService salePaymentCreateService;
    @Mock private SaleDeliveryAccessTokenService saleDeliveryAccessTokenService;
    @InjectMocks private SalePaymentDeliveryCreateService service;

    @Test
    void registersWebPaymentThroughExistingTransactionAndSalePaymentCores() throws Exception {
        ClientSessionDto client = new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente");
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "ST001";
        sale.StoreCod = "T001";
        sale.ClientCod = "CL001";
        sale.SaleStatus = StatusConst.PENDING;
        sale.CurrencyCod = "PEN";
        sale.NumExchangevalue = BigDecimal.ONE;
        sale.NumTotalPrice = new BigDecimal("25.00");

        TrxPaymentEntity requestedTransaction = new TrxPaymentEntity();
        requestedTransaction.TypeMovement = "I";
        requestedTransaction.PaymentMethodCod = "EF001";
        requestedTransaction.CurrencyCod = "USD";
        requestedTransaction.NumExchangevalue = new BigDecimal("3.50");
        requestedTransaction.AmountPaid = new BigDecimal("1.00");
        requestedTransaction.AmountReturned = new BigDecimal("5.00");
        TrxPaymentEntity savedTransaction = new TrxPaymentEntity();
        savedTransaction.TrxPaymentId = 15L;
        SalePaymentEntity expectedPayment = new SalePaymentEntity();

        SalePaymentDeliveryRegisterDto request = new SalePaymentDeliveryRegisterDto();
        request.OrderToken = "v1.order-token";
        request.TrxPayment = requestedTransaction;

        when(clientDeliveryContextService.getCurrentClient()).thenReturn(client);
        SaleDeliveryAccessTokenPayloadDto tokenPayload = new SaleDeliveryAccessTokenPayloadDto();
        tokenPayload.SaleCod = "ST001";
        tokenPayload.ClientCod = "CL001";
        when(saleDeliveryAccessTokenService.resolve("v1.order-token", "CL001"))
                .thenReturn(tokenPayload);
        when(saleHeadRepository.findWebSaleForUpdate("ST001", "CL001"))
                .thenReturn(Optional.of(sale));
        when(salePaymentRepository.countTotalPayment("ST001")).thenReturn(0);
        when(trxPaymentCreateService.saveWeb(requestedTransaction)).thenReturn(savedTransaction);
        when(salePaymentCreateService.saveWeb(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("T001")))
                .thenReturn(expectedPayment);

        SalePaymentEntity result = service.save(request);

        assertSame(expectedPayment, result);
        org.junit.jupiter.api.Assertions.assertEquals(StatusConst.PENDING, sale.SaleStatus);
        org.junit.jupiter.api.Assertions.assertEquals("PEN", requestedTransaction.CurrencyCod);
        org.junit.jupiter.api.Assertions.assertEquals(BigDecimal.ONE, requestedTransaction.NumExchangevalue);
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("25.00"), requestedTransaction.AmountPaid);
        org.junit.jupiter.api.Assertions.assertEquals(BigDecimal.ZERO, requestedTransaction.AmountReturned);
        verify(trxPaymentCreateService).saveWeb(requestedTransaction);
        ArgumentCaptor<SalePaymentRegisterDto> paymentCaptor =
                ArgumentCaptor.forClass(SalePaymentRegisterDto.class);
        verify(salePaymentCreateService).saveWeb(paymentCaptor.capture(), org.mockito.ArgumentMatchers.eq("T001"));
        org.junit.jupiter.api.Assertions.assertEquals("ST001", paymentCaptor.getValue().SaleCod);
        org.junit.jupiter.api.Assertions.assertEquals(15L, paymentCaptor.getValue().TrxPaymentId);
    }

    @Test
    void rejectsASecondPaymentForTheSameWebOrder() {
        ClientSessionDto client = new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente");
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "ST001";
        sale.StoreCod = "T001";
        sale.ClientCod = "CL001";
        sale.SaleStatus = StatusConst.PENDING;
        sale.CurrencyCod = "PEN";
        sale.NumExchangevalue = BigDecimal.ONE;
        sale.NumTotalPrice = new BigDecimal("25.00");

        TrxPaymentEntity requestedTransaction = new TrxPaymentEntity();
        requestedTransaction.TypeMovement = "I";
        requestedTransaction.PaymentMethodCod = "EF001";

        SalePaymentDeliveryRegisterDto request = new SalePaymentDeliveryRegisterDto();
        request.OrderToken = "v1.order-token";
        request.TrxPayment = requestedTransaction;

        when(clientDeliveryContextService.getCurrentClient()).thenReturn(client);
        SaleDeliveryAccessTokenPayloadDto tokenPayload = new SaleDeliveryAccessTokenPayloadDto();
        tokenPayload.SaleCod = "ST001";
        tokenPayload.ClientCod = "CL001";
        when(saleDeliveryAccessTokenService.resolve("v1.order-token", "CL001"))
                .thenReturn(tokenPayload);
        when(saleHeadRepository.findWebSaleForUpdate("ST001", "CL001"))
                .thenReturn(Optional.of(sale));
        when(salePaymentRepository.countTotalPayment("ST001")).thenReturn(1);

        assertThrows(IllegalArgumentException.class, () -> service.save(request));

        verify(trxPaymentCreateService, never()).saveWeb(requestedTransaction);
    }
}
