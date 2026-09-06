package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
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
    private KardexShared kardexShared;
    @Mock
    private ProductOperationConfigShared productOperationConfigShared;
    @InjectMocks
    private PucharseDetService pucharseDetService;

    @Test
    void confirmationAuditsTheNewDeliveryAsACreation() throws Exception {
        PucharseHeadEntity head = new PucharseHeadEntity();
        head.PucharseCod = "CO0001";
        head.StoreCod = "T001";
        head.PurchaseStatus = StatusConst.PENDING;

        PucharseDetEntity detail = new PucharseDetEntity();
        detail.PucharseCod = head.PucharseCod;
        detail.ItemNumber = 1;
        detail.ProductCod = "PR001";
        detail.Variant = "0000";
        detail.NumUnit = 5;
        detail.NumUnitPrice = new BigDecimal("10.00");
        detail.ProductUnitName = "NIU";
        detail.ProductUnitFactor = 1;
        detail.IsKardexAffected = "N";

        PucharseDetDeliveryEntity requestedDelivery =
                new PucharseDetDeliveryEntity();
        requestedDelivery.WarehouseCod = "A001";
        requestedDelivery.NumUnit = 5;

        PucharseDetConfirmDto request = new PucharseDetConfirmDto();
        request.pucharseDet = detail;
        request.pucharseDetDelivery = requestedDelivery;

        when(pucharseHeadRepository.findByIdForUpdate(head.PucharseCod))
                .thenReturn(Optional.of(head));
        when(pucharseDetRepository.findByIdForUpdate(head.PucharseCod, detail.ItemNumber))
                .thenReturn(Optional.of(detail));
        when(productOperationConfigShared.isDigital(detail.ProductCod, head.StoreCod))
                .thenReturn(false);
        when(kardexShared.buildPurchaseReceipt(any(), any(), any()))
                .thenReturn(List.<KardexEntity>of());
        when(kardexShared.buildZonePurchaseReceipt(any(), any(), any()))
                .thenReturn(List.<KardexZoneEntity>of());

        this.pucharseDetService.confirm(request);

        ArgumentCaptor<PucharseDetDeliveryEntity> captor =
                ArgumentCaptor.forClass(PucharseDetDeliveryEntity.class);
        verify(pucharseDetDeliveryRepository).save(captor.capture());
        assertEquals("SISTEMA", captor.getValue().CreationUser);
        assertNull(captor.getValue().ModifyUser);
        verify(kardexShared).saveAll(List.of(), List.of());
    }
}
