package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.KardexZoneShared;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditNoteStockConfirmationServiceTest {

    @Mock
    private KardexZoneShared kardexZoneShared;
    @Mock
    private KardexShared kardexShared;
    @InjectMocks
    private CreditNoteStockConfirmationService confirmationService;

    @Test
    void shouldAddCreditNoteStockToUnavailableZoneAndTotalKardex() throws Exception {
        CreditNoteHeadEntity head = head();
        CreditNoteDetEntity detail = detail(1, 10);
        WarehouseEntity warehouse = warehouse();
        this.mockNewMovement(20);
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);
        ArgumentCaptor<List<KardexEntity>> kardexCaptor = ArgumentCaptor.forClass(List.class);

        List<KardexEntity> result = this.confirmationService.addUnavailableStock(
                head,
                List.of(detail),
                warehouse,
                "USER01"
        );

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.SourceTable).isEqualTo(SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE);
        assertThat(operation.MovementEvent)
                .isEqualTo(SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION);
        assertThat(operation.MovementList).hasSize(1);
        assertThat(operation.MovementList.get(0).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_UNAVAILABLE);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(10);

        verify(this.kardexShared).saveAllLedgerOnly(kardexCaptor.capture());
        KardexEntity kardex = kardexCaptor.getValue().get(0);
        assertThat(kardex.TypeOperation).isEqualTo(KardexZoneConstants.TYPE_OPERATION_ADD);
        assertThat(kardex.NumStockBefore).isEqualTo(20);
        assertThat(kardex.NumStockAfter).isEqualTo(30);
        assertThat(kardex.LotNumber).isEqualTo("LOT-01");
        assertThat(result).containsExactly(kardex);
    }

    @Test
    void shouldNotDuplicateTotalKardexWhenConfirmationEventAlreadyExists() throws Exception {
        when(this.kardexZoneShared.apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        )).thenReturn(List.of());

        List<KardexEntity> result = this.confirmationService.addUnavailableStock(
                head(),
                List.of(detail(1, 10)),
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
    void shouldChainRepeatedProductLinesFromFreshKardexBalance() throws Exception {
        this.mockNewMovement(20);

        List<KardexEntity> result = this.confirmationService.addUnavailableStock(
                head(),
                List.of(detail(1, 10), detail(2, 5)),
                warehouse(),
                "USER01"
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).NumStockBefore).isEqualTo(20);
        assertThat(result.get(0).NumStockAfter).isEqualTo(30);
        assertThat(result.get(1).NumStockBefore).isEqualTo(30);
        assertThat(result.get(1).NumStockAfter).isEqualTo(35);
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

    private CreditNoteDetEntity detail(int itemNumber, int quantity) {
        CreditNoteDetEntity detail = new CreditNoteDetEntity();
        detail.CreditNoteCod = "NC001";
        detail.ItemNumber = itemNumber;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.NumUnit = quantity;
        detail.LotNumber = "LOT-01";
        return detail;
    }

    private WarehouseEntity warehouse() {
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.WarehouseCod = "W001";
        warehouse.StoreCod = "S001";
        return warehouse;
    }
}
