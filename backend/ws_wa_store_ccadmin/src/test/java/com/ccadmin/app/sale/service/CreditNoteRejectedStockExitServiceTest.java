package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditNoteRejectedStockExitServiceTest {

    @Mock
    private KardexZoneShared kardexZoneShared;
    @Mock
    private KardexShared kardexShared;
    @InjectMocks
    private CreditNoteRejectedStockExitService exitService;

    @Test
    void shouldSubtractRejectedUnitsFromUnavailableZoneAndTotalKardex() throws Exception {
        CreditNoteDetEntity detail = detail(1, 10, 6);
        this.mockConfirmation(detail);
        this.mockNewMovement(50);
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);
        ArgumentCaptor<List<KardexEntity>> kardexCaptor = ArgumentCaptor.forClass(List.class);

        List<KardexEntity> result = this.exitService.removeRejectedStock(
                head(),
                List.of(detail),
                warehouse(),
                "USER01"
        );

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.SourceTable).isEqualTo(SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE);
        assertThat(operation.MovementEvent)
                .isEqualTo(SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_REJECTED_STOCK_EXIT);
        assertThat(operation.MovementList).hasSize(1);
        assertThat(operation.MovementList.get(0).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_UNAVAILABLE);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(-4);

        verify(this.kardexShared).saveAllLedgerOnly(kardexCaptor.capture());
        KardexEntity kardex = kardexCaptor.getValue().get(0);
        assertThat(kardex.TypeOperation).isEqualTo(KardexZoneConstants.TYPE_OPERATION_SUBTRACT);
        assertThat(kardex.NumStockBefore).isEqualTo(50);
        assertThat(kardex.NumStockMoved).isEqualTo(4);
        assertThat(kardex.NumStockAfter).isEqualTo(46);
        assertThat(kardex.LotNumber).isEqualTo("LOT-01");
        assertThat(result).containsExactly(kardex);
    }

    @Test
    void shouldNotDuplicateTotalKardexWhenRejectedExitEventAlreadyExists() throws Exception {
        CreditNoteDetEntity detail = detail(1, 10, 6);
        this.mockConfirmation(detail);
        when(this.kardexZoneShared.apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        )).thenReturn(List.of());

        List<KardexEntity> result = this.exitService.removeRejectedStock(
                head(),
                List.of(detail),
                warehouse(),
                "USER01"
        );

        assertThat(result).isEmpty();
        verify(this.kardexShared, never()).findLastMovement(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(this.kardexShared, never()).saveAllLedgerOnly(anyList());
    }

    @Test
    void shouldSkipRejectedExitWhenEveryUnitWasAccepted() throws Exception {
        List<KardexEntity> result = this.exitService.removeRejectedStock(
                head(),
                List.of(detail(1, 10, 10)),
                warehouse(),
                "USER01"
        );

        assertThat(result).isEmpty();
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
        verify(this.kardexShared, never()).saveAllLedgerOnly(anyList());
    }

    @Test
    void shouldRequireOriginalUnavailableConfirmation() {
        CreditNoteDetEntity detail = detail(1, 10, 6);
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                "NC001",
                1,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        )).thenReturn(List.of());

        assertThatThrownBy(() -> this.exitService.removeRejectedStock(
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
        verify(this.kardexShared, never()).saveAllLedgerOnly(anyList());
    }

    @Test
    void shouldChainRepeatedProductLinesFromFreshKardexBalance() throws Exception {
        CreditNoteDetEntity first = detail(1, 10, 6);
        CreditNoteDetEntity second = detail(2, 8, 5);
        this.mockConfirmation(first);
        this.mockConfirmation(second);
        this.mockNewMovement(50);

        List<KardexEntity> result = this.exitService.removeRejectedStock(
                head(),
                List.of(first, second),
                warehouse(),
                "USER01"
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).NumStockBefore).isEqualTo(50);
        assertThat(result.get(0).NumStockAfter).isEqualTo(46);
        assertThat(result.get(1).NumStockBefore).isEqualTo(46);
        assertThat(result.get(1).NumStockAfter).isEqualTo(43);
    }

    @Test
    void shouldAllowHistoricalRejectedStockCoveredByUnavailableBaseline() throws Exception {
        CreditNoteHeadEntity head = head();
        head.CreationDate = new Date(1_700_000_000_000L);
        CreditNoteDetEntity detail = detail(1, 10, 6);
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                "NC001",
                1,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        )).thenReturn(List.of());
        when(this.kardexZoneShared.hasLegacyUnavailableBaseline(
                "P001", "0000", "S001", "W001", 10, head.CreationDate
        )).thenReturn(true);
        this.mockNewMovement(50);

        List<KardexEntity> result = this.exitService.removeRejectedStock(
                head,
                List.of(detail),
                warehouse(),
                "USER01"
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).NumStockMoved).isEqualTo(4);
    }

    private void mockConfirmation(CreditNoteDetEntity detail) {
        when(this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                "NC001",
                detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        )).thenReturn(List.of(confirmationMovement(detail)));
    }

    private void mockNewMovement(int totalStock) {
        KardexEntity previous = new KardexEntity();
        previous.NumStockAfter = totalStock;
        when(this.kardexZoneShared.apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        )).thenReturn(List.of(new KardexZoneEntity()));
        when(this.kardexShared.findLastMovement("P001", "0000", "W001", "S001"))
                .thenReturn(previous);
    }

    private CreditNoteHeadEntity head() {
        CreditNoteHeadEntity head = new CreditNoteHeadEntity();
        head.CreditNoteCod = "NC001";
        head.StoreCod = "S001";
        return head;
    }

    private CreditNoteDetEntity detail(int itemNumber, int quantity, int returned) {
        CreditNoteDetEntity detail = new CreditNoteDetEntity();
        detail.CreditNoteCod = "NC001";
        detail.ItemNumber = itemNumber;
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
