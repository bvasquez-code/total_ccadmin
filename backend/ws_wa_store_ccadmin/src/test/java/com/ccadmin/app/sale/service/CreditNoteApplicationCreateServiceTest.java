package com.ccadmin.app.sale.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.entity.CreditNoteApplicationEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.repository.CreditNoteApplicationRepository;
import com.ccadmin.app.sale.repository.CreditNoteDocumentRepository;
import com.ccadmin.app.sale.repository.CreditNoteHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditNoteApplicationCreateServiceTest {

    @Mock
    private CreditNoteHeadRepository creditNoteHeadRepository;
    @Mock
    private CreditNoteDocumentRepository creditNoteDocumentRepository;
    @Mock
    private CreditNoteApplicationRepository creditNoteApplicationRepository;
    @Mock
    private CreditNoteApplicationSearchService creditNoteApplicationSearchService;
    @Mock
    private TrxPaymentShared trxPaymentShared;
    @Mock
    private SalePaymentCreateService salePaymentCreateService;
    @Mock
    private SalePaymentRepository salePaymentRepository;
    @InjectMocks
    private CreditNoteApplicationCreateService applicationCreateService;

    @Test
    void appliesFullAvailableBalanceWhenNewSaleIsGreater() throws Exception {
        CreditNoteHeadEntity creditNote = exchangeCreditNote();
        SaleHeadEntity sale = newSale("120.00");
        CreditNoteDocumentEntity document = new CreditNoteDocumentEntity();
        document.CounterfoilCod = "BC01";
        document.DocumentCod = "000001";

        when(creditNoteHeadRepository.findByIdForUpdate(creditNote.CreditNoteCod))
                .thenReturn(Optional.of(creditNote));
        when(creditNoteApplicationSearchService.findAvailableBalance(creditNote))
                .thenReturn(new BigDecimal("100.00"));
        when(creditNoteDocumentRepository.findByCreditNoteCod(creditNote.CreditNoteCod))
                .thenReturn(document);
        when(trxPaymentShared.saveCreditNoteApplication(any(TrxPaymentEntity.class)))
                .thenAnswer(invocation -> {
                    TrxPaymentEntity payment = invocation.getArgument(0);
                    payment.TrxPaymentId = 77L;
                    return payment;
                });
        when(salePaymentCreateService.save(any(SalePaymentRegisterDto.class)))
                .thenReturn(new SalePaymentEntity());
        when(creditNoteApplicationRepository.save(any(CreditNoteApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreditNoteApplicationEntity result =
                applicationCreateService.applyAvailableBalance(creditNote.CreditNoteCod, sale);

        assertEquals(new BigDecimal("100.00"), result.AmountApplied);
        assertEquals(sale.SaleCod, result.SaleCod);
        assertEquals(77L, result.TrxPaymentId);
        assertEquals("S", creditNote.IsPaid);

        ArgumentCaptor<TrxPaymentEntity> paymentCaptor =
                ArgumentCaptor.forClass(TrxPaymentEntity.class);
        verify(trxPaymentShared).saveCreditNoteApplication(paymentCaptor.capture());
        assertEquals("NC001", paymentCaptor.getValue().PaymentMethodCod);
        assertEquals("CREDITO_INTERNO", paymentCaptor.getValue().PaymentPlatform);
        assertEquals(new BigDecimal("100.00"), paymentCaptor.getValue().AmountPaid);
    }

    @Test
    void rejectsSaleBelowAvailableBalance() {
        CreditNoteHeadEntity creditNote = exchangeCreditNote();
        SaleHeadEntity sale = newSale("99.99");

        when(creditNoteHeadRepository.findByIdForUpdate(creditNote.CreditNoteCod))
                .thenReturn(Optional.of(creditNote));
        when(creditNoteApplicationSearchService.findAvailableBalance(creditNote))
                .thenReturn(new BigDecimal("100.00"));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> applicationCreateService.applyAvailableBalance(
                        creditNote.CreditNoteCod,
                        sale
                )
        );

        assertEquals(
                "El total de la nueva venta debe ser igual o mayor al saldo de la nota de credito",
                exception.getMessage()
        );
        verify(trxPaymentShared, never())
                .saveCreditNoteApplication(any(TrxPaymentEntity.class));
    }

    @Test
    void releaseRestoresCreditAndInactivatesInternalPayment() throws Exception {
        CreditNoteHeadEntity creditNote = exchangeCreditNote();
        creditNote.IsPaid = "S";
        CreditNoteApplicationEntity application = new CreditNoteApplicationEntity();
        application.ApplicationId = 9L;
        application.CreditNoteCod = creditNote.CreditNoteCod;
        application.SaleCod = "VE002";
        application.TrxPaymentId = 77L;
        application.AmountApplied = new BigDecimal("100.00");

        SalePaymentEntity salePayment = new SalePaymentEntity();
        salePayment.SaleCod = application.SaleCod;
        salePayment.TrxPaymentId = application.TrxPaymentId;

        when(creditNoteApplicationSearchService.findActiveBySaleCod(application.SaleCod))
                .thenReturn(List.of(application));
        when(salePaymentRepository.findBySaleCod(application.SaleCod))
                .thenReturn(List.of(salePayment));
        when(creditNoteHeadRepository.findByIdForUpdate(creditNote.CreditNoteCod))
                .thenReturn(Optional.of(creditNote));

        int released = applicationCreateService.releaseBySale(application.SaleCod, "SYSTEM");

        assertEquals(1, released);
        assertEquals("I", application.Status);
        assertEquals("I", salePayment.Status);
        assertEquals("N", creditNote.IsPaid);
        verify(trxPaymentShared)
                .inactivateCreditNoteApplication(application.TrxPaymentId, "SYSTEM");
    }

    private CreditNoteHeadEntity exchangeCreditNote() {
        CreditNoteHeadEntity creditNote = new CreditNoteHeadEntity();
        creditNote.CreditNoteCod = "NC001";
        creditNote.CreditNoteStatus = SaleConstants.CONFIRMED;
        creditNote.IsProductExchange = "S";
        creditNote.IsPaid = "N";
        creditNote.Status = "A";
        creditNote.StoreCod = "T001";
        creditNote.ClientCod = "CL001";
        creditNote.CurrencyCod = "PEN";
        creditNote.NumTotalPrice = new BigDecimal("100.00");
        return creditNote;
    }

    private SaleHeadEntity newSale(String total) {
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "VE002";
        sale.StoreCod = "T001";
        sale.ClientCod = "CL001";
        sale.CurrencyCod = "PEN";
        sale.CurrencyCodSys = "PEN";
        sale.NumExchangevalue = BigDecimal.ONE;
        sale.NumTotalPrice = new BigDecimal(total);
        sale.SaleStatus = SaleConstants.PENDING;
        return sale;
    }
}
