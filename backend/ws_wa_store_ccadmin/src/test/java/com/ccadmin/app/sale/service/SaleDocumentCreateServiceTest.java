package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDocumentIssueDto;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.system.shared.CounterfoilShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleDocumentCreateServiceTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private SaleDocumentRepository saleDocumentRepository;
    @Mock
    private SaleSearchService saleSearchService;
    @Mock
    private CounterfoilShared counterfoilShared;
    @Mock
    private ClientShared clientShared;
    @Mock
    private GenericQueuedService genericQueuedService;
    @Mock
    private SaleSunatEmissionService saleSunatEmissionService;
    @Mock
    private SaleSunatPayloadBuildService saleSunatPayloadBuildService;
    @Mock
    private CatalogSearchShared catalogSearchShared;
    @InjectMocks
    private SaleDocumentCreateService saleDocumentCreateService;

    @Test
    void createsType99AsInternalDocumentWithoutFiscalFlag() throws Exception {
        SaleHeadEntity saleHead = confirmedPaidSale();
        SaleDocumentEntity generated = generatedDocument("P001-000001");
        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_PROFORMA_SALES
        )).thenReturn(true);
        when(saleDocumentRepository.countBySaleCodAndDocumentRoleAndStatus(
                saleHead.SaleCod,
                SaleConstants.DOCUMENT_ROLE_INTERNAL,
                "A"
        )).thenReturn(0L);
        when(counterfoilShared.generateDocumentSale(
                saleHead.StoreCod,
                SaleConstants.DOCUMENT_TYPE_PROFORMA,
                saleHead.SaleCod
        )).thenReturn(generated);
        when(saleDocumentRepository.save(generated)).thenReturn(generated);

        SaleDocumentEntity result = saleDocumentCreateService.createDocument(
                saleHead,
                SaleConstants.DOCUMENT_TYPE_PROFORMA
        );

        assertEquals(SaleConstants.DOCUMENT_TYPE_PROFORMA, result.DocumentType);
        assertEquals(SaleConstants.DOCUMENT_ROLE_INTERNAL, result.DocumentRole);
        assertEquals("N", saleHead.HasFiscalDocument);
        assertNotNull(result.IssueDate);
        verify(clientShared, never()).findById(any());
        verify(genericQueuedService, never()).addQueued(any());
    }

    @Test
    void rejectsProformaWhenIndicatorIsDisabled() {
        SaleHeadEntity saleHead = confirmedPaidSale();
        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_PROFORMA_SALES
        )).thenReturn(false);

        SaleException exception = assertThrows(
                SaleException.class,
                () -> saleDocumentCreateService.createDocument(
                        saleHead,
                        SaleConstants.DOCUMENT_TYPE_PROFORMA
                )
        );

        assertEquals("La emision de proformas no esta habilitada para esta empresa", exception.getMessage());
        verify(counterfoilShared, never()).generateDocumentSale(any(), any(), any());
    }

    @Test
    void issuesReceiptForConfirmedProformaAndMarksSaleAsFiscal() throws Exception {
        SaleHeadEntity saleHead = confirmedPaidSale();
        SaleDocumentEntity proforma = generatedDocument("P001-000001");
        proforma.DocumentType = SaleConstants.DOCUMENT_TYPE_PROFORMA;
        proforma.DocumentRole = SaleConstants.DOCUMENT_ROLE_INTERNAL;
        SaleDocumentEntity generatedReceipt = generatedDocument("B001-000001");
        SaleDetailDto expected = new SaleDetailDto();
        SaleDocumentIssueDto request = new SaleDocumentIssueDto();
        request.SaleCod = saleHead.SaleCod;
        request.DocumentType = SaleConstants.DOCUMENT_TYPE_RECEIPT;

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDocumentRepository.findFiscalBySaleCod(saleHead.SaleCod)).thenReturn(null);
        when(saleDocumentRepository.findProformaBySaleCod(saleHead.SaleCod)).thenReturn(proforma);
        when(saleDocumentRepository.countBySaleCodAndDocumentRoleAndStatus(
                saleHead.SaleCod,
                SaleConstants.DOCUMENT_ROLE_FISCAL,
                "A"
        )).thenReturn(0L);
        when(counterfoilShared.generateDocumentSale(
                saleHead.StoreCod,
                SaleConstants.DOCUMENT_TYPE_RECEIPT,
                saleHead.SaleCod
        )).thenReturn(generatedReceipt);
        when(saleDocumentRepository.save(generatedReceipt)).thenReturn(generatedReceipt);
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(expected);

        SaleDetailDto result = saleDocumentCreateService.issueFiscalDocument(request);

        assertEquals(expected, result);
        assertEquals("S", saleHead.HasFiscalDocument);
        assertEquals(SaleConstants.DOCUMENT_TYPE_RECEIPT, generatedReceipt.DocumentType);
        assertEquals(SaleConstants.DOCUMENT_ROLE_FISCAL, generatedReceipt.DocumentRole);
        verify(saleHeadRepository).save(saleHead);
        verify(genericQueuedService).addQueued(any(SaleSunatEmissionTaskService.class));
    }

    @Test
    void rejectsSecondFiscalDocumentBeforeConsumingCounterfoil() {
        SaleHeadEntity saleHead = confirmedPaidSale();
        saleHead.HasFiscalDocument = "S";
        SaleDocumentIssueDto request = new SaleDocumentIssueDto();
        request.SaleCod = saleHead.SaleCod;
        request.DocumentType = SaleConstants.DOCUMENT_TYPE_INVOICE;

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> saleDocumentCreateService.issueFiscalDocument(request)
        );

        assertEquals("La venta ya tiene una boleta o factura emitida", exception.getMessage());
        verify(counterfoilShared, never()).generateDocumentSale(any(), any(), any());
        verify(genericQueuedService, never()).addQueued(any());
    }

    private SaleHeadEntity confirmedPaidSale() {
        SaleHeadEntity saleHead = new SaleHeadEntity();
        saleHead.SaleCod = "ST001";
        saleHead.StoreCod = "001";
        saleHead.SaleStatus = SaleConstants.CONFIRMED;
        saleHead.IsPaid = "S";
        saleHead.HasFiscalDocument = "N";
        saleHead.NumTotalPrice = new BigDecimal("100.00");
        return saleHead;
    }

    private SaleDocumentEntity generatedDocument(String documentCod) {
        SaleDocumentEntity document = new SaleDocumentEntity();
        document.DocumentCod = documentCod;
        document.CounterfoilCod = "TL0001";
        document.SaleCod = "ST001";
        return document;
    }
}
