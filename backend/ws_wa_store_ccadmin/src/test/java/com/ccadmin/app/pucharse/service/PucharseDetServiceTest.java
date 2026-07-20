package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.pucharse.exception.PucharseException;
import com.ccadmin.app.pucharse.model.dto.PucharseDetConfirmDto;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.repository.PucharseDetDeliveryRepository;
import com.ccadmin.app.pucharse.repository.PucharseDetRepository;
import com.ccadmin.app.pucharse.repository.PucharseHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PucharseDetServiceTest {

    @Mock
    private PucharseHeadRepository pucharseHeadRepository;
    @Mock
    private PucharseDetRepository pucharseDetRepository;
    @Mock
    private PucharseDetDeliveryRepository pucharseDetDeliveryRepository;
    @Mock
    private PurchaseStockReceiptService purchaseStockReceiptService;
    @InjectMocks
    private PucharseDetService pucharseDetService;

    @Test
    void shouldLockAndReceivePurchaseDetailOnlyOnce() throws Exception {
        PucharseHeadEntity head = pendingPurchase();
        PucharseDetEntity detail = detail();
        PucharseDetConfirmDto request = request();
        when(this.pucharseHeadRepository.findByIdForUpdate("PC001")).thenReturn(Optional.of(head));
        when(this.pucharseDetRepository.findByIdForUpdate("PC001", 1)).thenReturn(Optional.of(detail));

        PucharseDetConfirmDto result = this.pucharseDetService.confirm(request);

        assertThat(result.pucharseDet).isSameAs(detail);
        assertThat(detail.IsKardexAffected).isEqualTo("S");
        assertThat(detail.NumUnitDelivered).isEqualTo(6);
        assertThat(request.pucharseDetDelivery.ProductCod).isEqualTo("P001");
        verify(this.purchaseStockReceiptService).receive(
                eq(head),
                eq(List.of(request.pucharseDetDelivery)),
                eq("SISTEMA")
        );
    }

    @Test
    void shouldRejectConcurrentRetryAfterDetailWasReceived() throws Exception {
        PucharseHeadEntity head = pendingPurchase();
        PucharseDetEntity detail = detail();
        detail.IsKardexAffected = "S";
        when(this.pucharseHeadRepository.findByIdForUpdate("PC001")).thenReturn(Optional.of(head));
        when(this.pucharseDetRepository.findByIdForUpdate("PC001", 1)).thenReturn(Optional.of(detail));

        assertThatThrownBy(() -> this.pucharseDetService.confirm(request()))
                .isInstanceOf(PucharseException.class)
                .hasMessageContaining("ya fue confirmado");

        verify(this.purchaseStockReceiptService, never()).receive(
                org.mockito.ArgumentMatchers.any(),
                anyList(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private PucharseHeadEntity pendingPurchase() {
        PucharseHeadEntity head = new PucharseHeadEntity();
        head.PucharseCod = "PC001";
        head.StoreCod = "S001";
        head.PurchaseStatus = StatusConst.PENDING;
        return head;
    }

    private PucharseDetEntity detail() {
        PucharseDetEntity detail = new PucharseDetEntity();
        detail.PucharseCod = "PC001";
        detail.ItemNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.ProductUnitName = "NIU";
        detail.ProductUnitFactor = 1;
        detail.NumUnit = 10;
        detail.IsKardexAffected = "N";
        return detail;
    }

    private PucharseDetConfirmDto request() {
        PucharseDetConfirmDto request = new PucharseDetConfirmDto();
        request.pucharseDet = detail();
        request.pucharseDetDelivery = new PucharseDetDeliveryEntity();
        request.pucharseDetDelivery.WarehouseCod = "W001";
        request.pucharseDetDelivery.NumUnit = 6;
        return request;
    }
}
