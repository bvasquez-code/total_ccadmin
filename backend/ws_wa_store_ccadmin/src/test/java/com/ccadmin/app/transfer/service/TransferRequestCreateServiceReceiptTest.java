package com.ccadmin.app.transfer.service;

import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.repository.WarehouseRepository;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.dto.TransferReceiveDto;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestHeadEntity;
import com.ccadmin.app.transfer.repository.TransferRequestDetRepository;
import com.ccadmin.app.transfer.repository.TransferRequestHeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferRequestCreateServiceReceiptTest {

    @Mock
    private TransferRequestHeadRepository transferRequestHeadRepository;
    @Mock
    private TransferRequestDetRepository transferRequestDetRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private TransferStockReceiptService transferStockReceiptService;
    @InjectMocks
    private TransferRequestCreateService transferRequestCreateService;

    @Test
    void shouldKeepDispatchConfirmedAndConfirmReceiveStatusSeparately() throws Exception {
        TransferRequestHeadEntity head = dispatchedHead();
        TransferRequestDetEntity detail = detail();
        TransferReceiveDto request = request(6);
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.WarehouseCod = "W002";
        warehouse.Status = "A";
        when(this.transferRequestHeadRepository.findByTransferCodAndTypeOperationForUpdate(
                "TR001",
                TransferConstants.TYPE_OPERATION_SEND
        )).thenReturn(head);
        when(this.transferRequestDetRepository.findByTransferCodAndTypeOperation(
                "TR001",
                TransferConstants.TYPE_OPERATION_SEND
        )).thenReturn(List.of(detail));
        when(this.warehouseRepository.findById("W002")).thenReturn(Optional.of(warehouse));

        this.transferRequestCreateService.receiveTransfer(request);

        assertThat(head.TransferStatus).isEqualTo(TransferConstants.STATUS_CONFIRMED);
        assertThat(head.ReceiveStatus).isEqualTo(TransferConstants.STATUS_CONFIRMED);
        assertThat(detail.NumUnitReception).isEqualTo(6);
        verify(this.transferStockReceiptService).receiveTransferRequest(
                eq("TR001"),
                eq("S002"),
                eq(List.of(detail)),
                eq("SISTEMA")
        );
    }

    @Test
    void shouldIgnoreReceiptRetryAfterReceiveStatusWasConfirmed() throws Exception {
        TransferRequestHeadEntity head = dispatchedHead();
        head.ReceiveStatus = TransferConstants.STATUS_CONFIRMED;
        when(this.transferRequestHeadRepository.findByTransferCodAndTypeOperationForUpdate(
                "TR001",
                TransferConstants.TYPE_OPERATION_SEND
        )).thenReturn(head);

        this.transferRequestCreateService.receiveTransfer(request(6));

        verify(this.transferRequestDetRepository, never()).findByTransferCodAndTypeOperation(
                "TR001",
                TransferConstants.TYPE_OPERATION_SEND
        );
        verify(this.transferStockReceiptService, never()).receiveTransferRequest(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private TransferRequestHeadEntity dispatchedHead() {
        TransferRequestHeadEntity head = new TransferRequestHeadEntity();
        head.TransferReqCod = "TR001";
        head.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
        head.StoreCodOrigin = "S001";
        head.StoreCodDest = "S002";
        head.TransferStatus = TransferConstants.STATUS_CONFIRMED;
        head.ReceiveStatus = TransferConstants.STATUS_PENDING;
        return head;
    }

    private TransferRequestDetEntity detail() {
        TransferRequestDetEntity detail = new TransferRequestDetEntity();
        detail.TransferReqCod = "TR001";
        detail.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
        detail.ItemNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.WarehouseCodDest = "W002";
        detail.NumUnit = 10;
        return detail;
    }

    private TransferReceiveDto request(int quantity) {
        TransferReceiveDto request = new TransferReceiveDto();
        request.transferCod = "TR001";
        TransferDetEntity received = new TransferDetEntity();
        received.ItemNumber = 1;
        received.NumUnitReception = quantity;
        request.detailListReceive = List.of(received);
        return request;
    }
}
