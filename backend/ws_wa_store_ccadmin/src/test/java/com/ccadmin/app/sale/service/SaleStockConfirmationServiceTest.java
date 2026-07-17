package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
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
class SaleStockConfirmationServiceTest {

    @Mock
    private KardexZoneShared kardexZoneShared;
    @InjectMocks
    private SaleStockConfirmationService confirmationService;

    @Test
    void shouldConsumeReservedStockAndRegisterPhysicalExit() throws SaleException {
        SaleHeadEntity sale = sale();
        SaleDetWarehouseEntity detail = detail();
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                "PS001",
                1,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        )).thenReturn(reservation(detail));
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);

        this.confirmationService.consumeReservation(sale, List.of(detail), "USER01");

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.OperationCod).isEqualTo("SL001");
        assertThat(operation.SourceTable).isEqualTo(SaleConstants.KARDEX_ZONE_SOURCE_SALE);
        assertThat(operation.MovementEvent).isEqualTo(SaleConstants.KARDEX_ZONE_EVENT_CONFIRMATION);
        assertThat(operation.MovementList).hasSize(3);
        assertThat(operation.MovementList.get(0).ZoneStockMoved).isEqualTo(KardexZoneConstants.ZONE_RESERVED);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(-10);
        assertThat(operation.MovementList.get(1).ZoneStockMoved).isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(operation.MovementList.get(1).NumStockDelta).isEqualTo(10);
        assertThat(operation.MovementList.get(2).ZoneStockMoved).isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(operation.MovementList.get(2).NumStockDelta).isEqualTo(-10);
    }

    @Test
    void shouldRejectSaleWhenReservationDoesNotExist() {
        SaleHeadEntity sale = sale();
        SaleDetWarehouseEntity detail = detail();
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                "PS001",
                1,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        )).thenReturn(List.of());

        assertThatThrownBy(() ->
                this.confirmationService.consumeReservation(sale, List.of(detail), "USER01")
        ).isInstanceOf(SaleException.class)
                .hasMessageContaining("reserva valida");

        verify(this.kardexZoneShared, never()).apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        );
    }

    private List<KardexZoneEntity> reservation(SaleDetWarehouseEntity detail) {
        KardexZoneEntity physical = movement(
                detail,
                KardexZoneConstants.ZONE_PHYSICAL,
                KardexZoneConstants.TYPE_OPERATION_SUBTRACT
        );
        KardexZoneEntity reserved = movement(
                detail,
                KardexZoneConstants.ZONE_RESERVED,
                KardexZoneConstants.TYPE_OPERATION_ADD
        );
        return List.of(physical, reserved);
    }

    private KardexZoneEntity movement(
            SaleDetWarehouseEntity detail,
            String zone,
            String typeOperation
    ) {
        KardexZoneEntity movement = new KardexZoneEntity();
        movement.ProductCod = detail.ProductCod;
        movement.Variant = detail.Variant;
        movement.WarehouseCod = detail.WarehouseCod;
        movement.ZoneStockMoved = zone;
        movement.TypeOperation = typeOperation;
        movement.NumStockMoved = detail.NumUnit;
        return movement;
    }

    private SaleHeadEntity sale() {
        SaleHeadEntity entity = new SaleHeadEntity();
        entity.SaleCod = "SL001";
        entity.PresaleCod = "PS001";
        entity.StoreCod = "S001";
        entity.SaleStatus = SaleConstants.PENDING;
        return entity;
    }

    private SaleDetWarehouseEntity detail() {
        SaleDetWarehouseEntity entity = new SaleDetWarehouseEntity();
        entity.SaleCod = "SL001";
        entity.ItemNumber = 1;
        entity.ProductCod = "P001";
        entity.Variant = "0000";
        entity.WarehouseCod = "W001";
        entity.NumUnit = 10;
        return entity;
    }
}
