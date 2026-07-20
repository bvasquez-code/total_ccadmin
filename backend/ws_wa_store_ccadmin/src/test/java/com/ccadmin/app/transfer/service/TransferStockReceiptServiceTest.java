package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
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
class TransferStockReceiptServiceTest {

    @Mock
    private KardexZoneShared kardexZoneShared;
    @Mock
    private KardexShared kardexShared;
    @InjectMocks
    private TransferStockReceiptService receiptService;

    @Test
    void shouldAddReceivedQuantityToPhysicalAndTotalStock() throws Exception {
        TransferDetEntity detail = transferDetail(1, 10);
        this.mockNewMovement(20);
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);
        ArgumentCaptor<List<KardexEntity>> kardexCaptor = ArgumentCaptor.forClass(List.class);

        List<KardexEntity> result = this.receiptService.receiveTransfer(
                "TF001",
                "S002",
                List.of(detail),
                "USER01"
        );

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        KardexZoneOperationDto operation = operationCaptor.getValue();
        assertThat(operation.SourceTable).isEqualTo(TransferConstants.KARDEX_SOURCE_TABLE);
        assertThat(operation.MovementEvent).isEqualTo(TransferConstants.KARDEX_ZONE_EVENT_RECEIPT);
        assertThat(operation.MovementList).hasSize(1);
        assertThat(operation.MovementList.get(0).ZoneStockMoved)
                .isEqualTo(KardexZoneConstants.ZONE_PHYSICAL);
        assertThat(operation.MovementList.get(0).NumStockDelta).isEqualTo(10);

        verify(this.kardexShared).saveAllLedgerOnly(kardexCaptor.capture());
        KardexEntity kardex = kardexCaptor.getValue().get(0);
        assertThat(kardex.TypeOperation).isEqualTo(TransferConstants.KARDEX_TYPE_IN);
        assertThat(kardex.NumStockBefore).isEqualTo(20);
        assertThat(kardex.NumStockMoved).isEqualTo(10);
        assertThat(kardex.NumStockAfter).isEqualTo(30);
        assertThat(result).containsExactly(kardex);
    }

    @Test
    void shouldUseRequestSourceForTransferRequestReceipt() throws Exception {
        TransferRequestDetEntity detail = requestDetail(1, 8);
        this.mockNewMovement(20);
        ArgumentCaptor<KardexZoneOperationDto> operationCaptor =
                ArgumentCaptor.forClass(KardexZoneOperationDto.class);

        this.receiptService.receiveTransferRequest(
                "TR001",
                "S002",
                List.of(detail),
                "USER01"
        );

        verify(this.kardexZoneShared).apply(operationCaptor.capture(), eq("USER01"));
        assertThat(operationCaptor.getValue().SourceTable)
                .isEqualTo(TransferConstants.KARDEX_ZONE_SOURCE_REQUEST);
        assertThat(operationCaptor.getValue().MovementList.get(0).NumStockDelta).isEqualTo(8);
    }

    @Test
    void shouldNotDuplicateKardexWhenReceiptEventAlreadyExists() throws Exception {
        TransferDetEntity detail = transferDetail(1, 10);
        when(this.kardexZoneShared.apply(
                org.mockito.ArgumentMatchers.any(KardexZoneOperationDto.class),
                eq("USER01")
        )).thenReturn(List.of());

        List<KardexEntity> result = this.receiptService.receiveTransfer(
                "TF001",
                "S002",
                List.of(detail),
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
    void shouldChainRepeatedProductLinesAtDestination() throws Exception {
        TransferDetEntity first = transferDetail(1, 10);
        TransferDetEntity second = transferDetail(2, 5);
        this.mockNewMovement(20);

        List<KardexEntity> result = this.receiptService.receiveTransfer(
                "TF001",
                "S002",
                List.of(first, second),
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
        when(this.kardexShared.findLastMovement("P001", "0000", "W002", "S002"))
                .thenReturn(previous);
    }

    private TransferDetEntity transferDetail(int itemNumber, int quantity) {
        TransferDetEntity detail = new TransferDetEntity();
        detail.TransferCod = "TF001";
        detail.ItemNumber = itemNumber;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.WarehouseCodDest = "W002";
        detail.NumUnitReception = quantity;
        detail.LotNumber = "LOT-01";
        return detail;
    }

    private TransferRequestDetEntity requestDetail(int itemNumber, int quantity) {
        TransferRequestDetEntity detail = new TransferRequestDetEntity();
        detail.TransferReqCod = "TR001";
        detail.ItemNumber = itemNumber;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.WarehouseCodDest = "W002";
        detail.NumUnitReception = quantity;
        detail.LotNumber = "LOT-01";
        return detail;
    }
}
