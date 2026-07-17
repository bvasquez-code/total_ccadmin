package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.sale.exception.PresaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleDetWarehouseRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresaleStockReservationServiceTest {

    @Mock
    private PresaleDetWarehouseRepository presaleDetWarehouseRepository;
    @Mock
    private KardexZoneShared kardexZoneShared;
    @InjectMocks
    private PresaleStockReservationService reservationService;

    @Test
    void shouldCreatePhysicalAndReservedMovementsForConfirmedPresale() throws PresaleException {
        PresaleHeadEntity presale = confirmedPresale();
        SaleHeadEntity sale = pendingSale();
        PresaleDetWarehouseEntity detail = detail();
        when(this.presaleDetWarehouseRepository.findActiveByPresaleCod("PS001"))
                .thenReturn(List.of(detail));
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);

        this.reservationService.reserve(presale, sale, "USER01");

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.OperationCod).isEqualTo("PS001");
        assertThat(operation.SourceTable).isEqualTo(SaleConstants.KARDEX_ZONE_SOURCE_PRESALE);
        assertThat(operation.MovementEvent).isEqualTo(SaleConstants.KARDEX_ZONE_EVENT_RESERVATION);
        assertThat(operation.MovementList).hasSize(2);
        assertThat(operation.MovementList.get(0).ZoneStockMoved).isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(-10);
        assertThat(operation.MovementList.get(1).ZoneStockMoved).isEqualTo(KardexZoneConstants.ZONE_RESERVED);
        assertThat(operation.MovementList.get(1).NumStockDelta).isEqualTo(10);
    }

    @Test
    void shouldRejectReservationWithoutPendingSale() {
        PresaleHeadEntity presale = confirmedPresale();
        SaleHeadEntity sale = pendingSale();
        sale.SaleStatus = StatusConst.CONFIRMED;

        assertThatThrownBy(() -> this.reservationService.reserve(presale, sale, "USER01"))
                .isInstanceOf(PresaleException.class)
                .hasMessageContaining("venta pendiente");

        verify(this.presaleDetWarehouseRepository, never()).findActiveByPresaleCod("PS001");
    }

    private PresaleHeadEntity confirmedPresale() {
        PresaleHeadEntity entity = new PresaleHeadEntity();
        entity.PresaleCod = "PS001";
        entity.StoreCod = "S001";
        entity.SaleStatus = StatusConst.CONFIRMED;
        return entity;
    }

    private SaleHeadEntity pendingSale() {
        SaleHeadEntity entity = new SaleHeadEntity();
        entity.SaleCod = "SL001";
        entity.PresaleCod = "PS001";
        entity.StoreCod = "S001";
        entity.SaleStatus = StatusConst.PENDING;
        return entity;
    }

    private PresaleDetWarehouseEntity detail() {
        PresaleDetWarehouseEntity entity = new PresaleDetWarehouseEntity();
        entity.PresaleCod = "PS001";
        entity.ItemNumber = 1;
        entity.ProductCod = "P001";
        entity.Variant = "0000";
        entity.WarehouseCod = "W001";
        entity.NumUnit = 10;
        return entity;
    }
}
