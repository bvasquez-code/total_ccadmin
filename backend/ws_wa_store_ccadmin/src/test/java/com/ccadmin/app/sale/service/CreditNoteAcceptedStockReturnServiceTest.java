package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditNoteAcceptedStockReturnServiceTest {

    @Mock
    private KardexZoneShared kardexZoneShared;
    @InjectMocks
    private CreditNoteAcceptedStockReturnService returnService;

    @Test
    void shouldMoveAcceptedUnitsFromUnavailableToPhysicalWithoutChangingTotalStock() throws Exception {
        CreditNoteDetEntity detail = detail(10, 6);
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                "NC001",
                1,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        )).thenReturn(List.of(confirmationMovement(detail)));
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);

        this.returnService.moveAcceptedStockToPhysical(
                head(),
                List.of(detail),
                warehouse(),
                "USER01"
        );

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.SourceTable).isEqualTo(SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE);
        assertThat(operation.MovementEvent)
                .isEqualTo(SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_ACCEPTED_RETURN);
        assertThat(operation.LotNumber).isEqualTo("LOT-01");
        assertThat(operation.MovementList).hasSize(2);
        assertThat(operation.MovementList.get(0).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_UNAVAILABLE);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(-6);
        assertThat(operation.MovementList.get(1).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(operation.MovementList.get(1).NumStockDelta).isEqualTo(6);
        assertThat(operation.MovementList.stream().mapToInt(m -> m.NumStockDelta).sum()).isZero();
    }

    @Test
    void shouldRequireOriginalUnavailableConfirmation() {
        CreditNoteDetEntity detail = detail(10, 6);
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                "NC001",
                1,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        )).thenReturn(List.of());

        assertThatThrownBy(() -> this.returnService.moveAcceptedStockToPhysical(
                head(),
                List.of(detail),
                warehouse(),
                "USER01"
        )).isInstanceOf(SaleException.class)
                .hasMessageContaining("No existe stock no disponible confirmado");

        verify(this.kardexZoneShared, never()).apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldSkipItemsWithoutAcceptedUnits() throws Exception {
        this.returnService.moveAcceptedStockToPhysical(
                head(),
                List.of(detail(10, 0)),
                warehouse(),
                "USER01"
        );

        verify(this.kardexZoneShared, never()).findByEvent(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(this.kardexZoneShared, never()).apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldAllowHistoricalCreditNoteCoveredByUnavailableBaseline() throws Exception {
        CreditNoteHeadEntity head = head();
        head.CreationDate = new Date(1_700_000_000_000L);
        CreditNoteDetEntity detail = detail(10, 6);
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                "NC001",
                1,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        )).thenReturn(List.of());
        when(this.kardexZoneShared.hasLegacyUnavailableBaseline(
                "P001", "0000", "S001", "W001", 10, head.CreationDate
        )).thenReturn(true);

        this.returnService.moveAcceptedStockToPhysical(
                head,
                List.of(detail),
                warehouse(),
                "USER01"
        );

        verify(this.kardexZoneShared).apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        );
    }

    private CreditNoteHeadEntity head() {
        CreditNoteHeadEntity head = new CreditNoteHeadEntity();
        head.CreditNoteCod = "NC001";
        head.StoreCod = "S001";
        return head;
    }

    private CreditNoteDetEntity detail(int quantity, int returned) {
        CreditNoteDetEntity detail = new CreditNoteDetEntity();
        detail.CreditNoteCod = "NC001";
        detail.ItemNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.NumUnit = quantity;
        detail.NumUnitStockReturned = returned;
        detail.LotNumber = "LOT-01";
        return detail;
    }

    private KardexZoneEntity confirmationMovement(CreditNoteDetEntity detail) {
        KardexZoneEntity movement = new KardexZoneEntity();
        movement.ProductCod = detail.ProductCod;
        movement.Variant = detail.Variant;
        movement.WarehouseCod = "W001";
        movement.ZoneStockMoved = KardexZoneConstants.ZONE_UNAVAILABLE;
        movement.TypeOperation = KardexZoneConstants.TYPE_OPERATION_ADD;
        movement.NumStockMoved = detail.NumUnit;
        return movement;
    }

    private WarehouseEntity warehouse() {
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.WarehouseCod = "W001";
        warehouse.StoreCod = "S001";
        return warehouse;
    }
}
