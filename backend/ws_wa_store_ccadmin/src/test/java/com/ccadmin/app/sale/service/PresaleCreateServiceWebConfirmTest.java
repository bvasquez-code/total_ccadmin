package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SalesContextDto;
import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleChannelRepository;
import com.ccadmin.app.sale.repository.PresaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresaleCreateServiceWebConfirmTest {

    @Mock private PresaleHeadRepository presaleHeadRepository;
    @Mock private PresaleChannelRepository presaleChannelRepository;
    @Mock private PresaleDetWarehouseRepository presaleDetWarehouseRepository;
    @Mock private PresaleSearchService presaleSearchService;
    @Mock private SaleCreateService saleCreateService;
    @Mock private KardexShared kardexShared;
    @Mock private SalesContextService salesContextService;
    @InjectMocks private PresaleCreateService presaleCreateService;

    @Test
    void confirmsWebPresaleAndCreatesPendingSaleThroughTheNormalCore() throws Exception {
        PresaleRegisterDto request = new PresaleRegisterDto();
        request.Headboard = new PresaleHeadEntity();
        request.Headboard.PresaleCod = "PT001";
        request.Headboard.StoreCod = "T001";

        PresaleHeadEntity storedPresale = new PresaleHeadEntity();
        storedPresale.PresaleCod = "PT001";
        storedPresale.StoreCod = "T001";
        storedPresale.ClientCod = "CL001";
        storedPresale.SaleStatus = StatusConst.PENDING;

        PresaleChannelEntity channel = new PresaleChannelEntity();
        channel.PresaleCod = "PT001";
        channel.ChannelCod = SaleConstants.COMMERCIAL_CHANNEL_WEB;

        PresaleDetailDto presaleDetail = new PresaleDetailDto();
        presaleDetail.Headboard = storedPresale;
        SaleDetailDto expectedSale = new SaleDetailDto();
        expectedSale.Headboard = new SaleHeadEntity();
        expectedSale.Headboard.SaleCod = "ST001";
        expectedSale.Headboard.PresaleCod = "PT001";
        expectedSale.Headboard.SaleStatus = StatusConst.PENDING;

        when(salesContextService.getWebContext("T001"))
                .thenReturn(new SalesContextDto("T001", "USER_WEB", null));
        when(presaleHeadRepository.findByIdForUpdate("PT001"))
                .thenReturn(Optional.of(storedPresale));
        when(presaleChannelRepository.findByPresaleCod("PT001"))
                .thenReturn(Optional.of(channel));
        when(presaleSearchService.findById("PT001")).thenReturn(presaleDetail);
        when(saleCreateService.saveWeb(presaleDetail, "T001")).thenReturn(expectedSale);
        when(presaleDetWarehouseRepository.findActiveByPresaleCod("PT001"))
                .thenReturn(List.of(new PresaleDetWarehouseEntity()));
        when(kardexShared.buildPresaleReservation(any(), any(), any()))
                .thenReturn(List.<KardexZoneEntity>of());

        SaleDetailDto result = presaleCreateService.confirmWeb(request, "T001", "CL001");

        assertSame(expectedSale, result);
        assertEquals(StatusConst.CONFIRMED, storedPresale.SaleStatus);
        assertEquals(StatusConst.PENDING, result.Headboard.SaleStatus);
        verify(presaleHeadRepository).save(storedPresale);
        verify(saleCreateService).saveWeb(presaleDetail, "T001");
    }
}
