package com.ccadmin.app.sale.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.dto.SalesContextDto;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalePaymentCreateServiceWebTest {

    @Mock private SalePaymentRepository salePaymentRepository;
    @Mock private SaleHeadRepository saleHeadRepository;
    @Mock private TrxPaymentShared trxPaymentShared;
    @Mock private SalesContextService salesContextService;
    @InjectMocks private SalePaymentCreateService salePaymentCreateService;

    @Test
    void marksSaleAsPaidWithoutExecutingItsFinalConfirmation() throws Exception {
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "ST001";
        sale.StoreCod = "T001";
        sale.SaleStatus = StatusConst.PENDING;
        sale.IsPaid = "N";
        sale.CurrencyCodSys = "PEN";
        sale.NumExchangevalue = BigDecimal.ONE;
        sale.NumTotalPrice = new BigDecimal("100.00");

        TrxPaymentEntity transaction = new TrxPaymentEntity();
        transaction.TrxPaymentId = 20L;
        transaction.CurrencyCod = "PEN";
        transaction.NumExchangevalue = BigDecimal.ONE;
        transaction.AmountPaid = new BigDecimal("100.00");

        SalePaymentRegisterDto request = new SalePaymentRegisterDto();
        request.SaleCod = "ST001";
        request.TrxPaymentId = 20L;

        when(salesContextService.getWebContext("T001"))
                .thenReturn(new SalesContextDto("T001", "USER_WEB", null));
        when(saleHeadRepository.findByIdForUpdate("ST001")).thenReturn(Optional.of(sale));
        when(trxPaymentShared.findById(20L)).thenReturn(transaction);
        when(salePaymentRepository.findTotalPayment("ST001")).thenReturn(BigDecimal.ZERO);
        when(salePaymentRepository.countTotalPayment("ST001")).thenReturn(0);

        salePaymentCreateService.saveWeb(request, "T001");

        assertEquals("S", sale.IsPaid);
        assertEquals(StatusConst.PENDING, sale.SaleStatus);
        verify(saleHeadRepository).save(sale);
    }
}
