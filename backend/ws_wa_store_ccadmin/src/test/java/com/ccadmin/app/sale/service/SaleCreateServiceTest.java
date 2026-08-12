package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.service.ProductRankingService;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import com.ccadmin.app.sale.model.entity.SaleChannelEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.repository.PresaleChannelRepository;
import com.ccadmin.app.sale.repository.SaleChannelRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleCreateServiceTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private PresaleChannelRepository presaleChannelRepository;
    @Mock
    private SaleChannelRepository saleChannelRepository;
    @Mock
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Mock
    private SaleSearchService saleSearchService;
    @Mock
    private GenericQueuedService genericQueuedService;
    @Mock
    private ProductRankingService productRankingService;
    @Mock
    private KardexShared kardexShared;
    @Mock
    private SaleDocumentCreateService saleDocumentCreateService;
    @Mock
    private CatalogSearchShared catalogSearchShared;
    @Mock
    private SaleDeliveryRepository saleDeliveryRepository;
    @InjectMocks
    private SaleCreateService saleCreateService;


    @Test
    void confirmsProformaWithoutSchedulingSunat() throws Exception {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetWarehouseEntity warehouseDetail = new SaleDetWarehouseEntity();
        SaleDocumentEntity proforma = new SaleDocumentEntity();
        proforma.DocumentCod = "P001-000001";
        proforma.DocumentType = SaleConstants.DOCUMENT_TYPE_PROFORMA;
        proforma.DocumentRole = SaleConstants.DOCUMENT_ROLE_INTERNAL;
        SaleDetailDto expected = new SaleDetailDto();

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetWarehouseRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of(warehouseDetail));
        when(kardexShared.buildSaleConfirmation(any(), any(), any())).thenReturn(List.<KardexEntity>of());
        when(kardexShared.buildZoneSaleConfirmation(any(), any(), any())).thenReturn(List.<KardexZoneEntity>of());
        when(saleDocumentCreateService.createDocument(
                saleHead,
                SaleConstants.DOCUMENT_TYPE_PROFORMA
        )).thenReturn(proforma);
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(expected);

        SaleDetailDto result = saleCreateService.confirm(
                saleHead.SaleCod,
                SaleConstants.DOCUMENT_TYPE_PROFORMA,
                ""
        );

        assertEquals(expected, result);
        assertEquals(SaleConstants.CONFIRMED, saleHead.SaleStatus);
        verify(saleDocumentCreateService, never()).emitSunatAfterCommit(any(), any());
    }

    @Test
    void confirmsInvoiceAndSchedulesItsExactDocument() throws Exception {
        SaleHeadEntity saleHead = pendingSale();
        SaleDetWarehouseEntity warehouseDetail = new SaleDetWarehouseEntity();
        SaleDocumentEntity invoice = new SaleDocumentEntity();
        invoice.DocumentCod = "F001-000001";
        invoice.DocumentType = SaleConstants.DOCUMENT_TYPE_INVOICE;
        invoice.DocumentRole = SaleConstants.DOCUMENT_ROLE_FISCAL;

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetWarehouseRepository.findBySaleCod(saleHead.SaleCod)).thenReturn(List.of(warehouseDetail));
        when(kardexShared.buildSaleConfirmation(any(), any(), any())).thenReturn(List.<KardexEntity>of());
        when(kardexShared.buildZoneSaleConfirmation(any(), any(), any())).thenReturn(List.<KardexZoneEntity>of());
        when(saleDocumentCreateService.createDocument(
                saleHead,
                SaleConstants.DOCUMENT_TYPE_INVOICE
        )).thenReturn(invoice);
        when(saleSearchService.findById(saleHead.SaleCod)).thenReturn(new SaleDetailDto());

        saleCreateService.confirm(saleHead.SaleCod, SaleConstants.DOCUMENT_TYPE_INVOICE, "");

        verify(saleDocumentCreateService).emitSunatAfterCommit(saleHead.SaleCod, invoice.DocumentCod);
    }

    @Test
    void rejectsSaleConfirmationWhenPickingIsMandatoryAndNotConfirmed() throws Exception {
        SaleHeadEntity saleHead = pendingSale();
        saleHead.IsPickingConfirmed = "N";

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetWarehouseRepository.findBySaleCod(saleHead.SaleCod))
                .thenReturn(List.of(new SaleDetWarehouseEntity()));
        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_MANDATORY_PICKING
        )).thenReturn(true);

        SaleException exception = assertThrows(
                SaleException.class,
                () -> saleCreateService.confirm(saleHead.SaleCod, SaleConstants.DOCUMENT_TYPE_INVOICE, "")
        );

        assertEquals(
                "Debe confirmar el pickeo de todos los productos antes de confirmar la venta",
                exception.getMessage()
        );
        verify(kardexShared, never()).buildSaleConfirmation(any(), any(), any());
        verify(saleDocumentCreateService, never()).createDocument(any(), any());
    }

    @Test
    void rejectsWebConfirmationBeforeDeliveryPreparationStarts() {
        SaleHeadEntity saleHead = pendingSale();
        saleHead.IsPaid = "S";
        SaleChannelEntity channel = new SaleChannelEntity();
        channel.SaleCod = saleHead.SaleCod;
        channel.ChannelCod = SaleConstants.COMMERCIAL_CHANNEL_WEB;
        SaleDeliveryEntity delivery = new SaleDeliveryEntity();
        delivery.SaleCod = saleHead.SaleCod;
        delivery.DeliveryStatus = SaleConstants.DELIVERY_STATUS_PENDING;

        when(saleHeadRepository.findByIdForUpdate(saleHead.SaleCod)).thenReturn(Optional.of(saleHead));
        when(saleDetWarehouseRepository.findBySaleCod(saleHead.SaleCod))
                .thenReturn(List.of(new SaleDetWarehouseEntity()));
        when(saleChannelRepository.findActiveBySaleCod(saleHead.SaleCod))
                .thenReturn(Optional.of(channel));
        when(saleDeliveryRepository.findActiveBySaleCod(saleHead.SaleCod))
                .thenReturn(Optional.of(delivery));

        SaleException exception = assertThrows(
                SaleException.class,
                () -> saleCreateService.confirm(
                        saleHead.SaleCod,
                        SaleConstants.DOCUMENT_TYPE_RECEIPT,
                        ""
                )
        );

        assertEquals(
                "Debe iniciar la preparacion del pedido antes de facturarlo",
                exception.getMessage()
        );
        verify(kardexShared, never()).buildSaleConfirmation(any(), any(), any());
    }

    private SaleHeadEntity pendingSale() {
        SaleHeadEntity saleHead = new SaleHeadEntity();
        saleHead.SaleCod = "ST001";
        saleHead.StoreCod = "001";
        saleHead.SaleStatus = SaleConstants.PENDING;
        return saleHead;
    }
}
