package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransferStockReceiptService {

    @Autowired
    private KardexZoneShared kardexZoneShared;
    @Autowired
    private KardexShared kardexShared;

    @Transactional(rollbackOn = Exception.class)
    public List<KardexEntity> receiveTransfer(
            String transferCod,
            String storeCodDest,
            List<TransferDetEntity> detailList,
            String userCod
    ) throws TransferException {
        List<ReceiptLine> lineList = detailList.stream()
                .filter(detail -> detail.NumUnitReception > 0)
                .map(detail -> new ReceiptLine(
                        detail.ItemNumber,
                        detail.ProductCod,
                        detail.Variant,
                        detail.WarehouseCodDest,
                        detail.NumUnitReception,
                        detail.LotNumber,
                        detail.ExpirationDate
                ))
                .toList();
        return this.receive(
                transferCod,
                TransferConstants.KARDEX_SOURCE_TABLE,
                storeCodDest,
                lineList,
                userCod
        );
    }

    @Transactional(rollbackOn = Exception.class)
    public List<KardexEntity> receiveTransferRequest(
            String transferReqCod,
            String storeCodDest,
            List<TransferRequestDetEntity> detailList,
            String userCod
    ) throws TransferException {
        List<ReceiptLine> lineList = detailList.stream()
                .filter(detail -> detail.NumUnitReception > 0)
                .map(detail -> new ReceiptLine(
                        detail.ItemNumber,
                        detail.ProductCod,
                        detail.Variant,
                        detail.WarehouseCodDest,
                        detail.NumUnitReception,
                        detail.LotNumber,
                        detail.ExpirationDate
                ))
                .toList();
        return this.receive(
                transferReqCod,
                TransferConstants.KARDEX_ZONE_SOURCE_REQUEST,
                storeCodDest,
                lineList,
                userCod
        );
    }

    private List<KardexEntity> receive(
            String operationCod,
            String sourceTable,
            String storeCodDest,
            List<ReceiptLine> lineList,
            String userCod
    ) throws TransferException {
        if (operationCod == null || operationCod.isBlank()) {
            throw new TransferException("El codigo de transferencia es obligatorio");
        }
        if (lineList == null || lineList.isEmpty()) {
            throw new TransferException("La transferencia no tiene unidades para recibir");
        }

        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (ReceiptLine line : lineList) {
            if (this.kardexZoneShared.apply(
                    this.buildReceipt(operationCod, sourceTable, storeCodDest, line),
                    userCod
            ).isEmpty()) {
                continue;
            }

            String stockKey = this.stockKey(storeCodDest, line);
            KardexEntity lastMovement = lastMovementByStock.computeIfAbsent(
                    stockKey,
                    ignored -> this.kardexShared.findLastMovement(
                            line.productCod,
                            line.variant,
                            line.warehouseCod,
                            storeCodDest
                    )
            );
            KardexEntity kardex = this.buildKardex(
                    lastMovement,
                    operationCod,
                    storeCodDest,
                    line,
                    userCod
            );
            kardexList.add(kardex);
            lastMovementByStock.put(stockKey, kardex);
        }

        if (!kardexList.isEmpty()) {
            this.kardexShared.saveAllLedgerOnly(kardexList);
        }
        return kardexList;
    }

    private KardexZoneOperationDto buildReceipt(
            String operationCod,
            String sourceTable,
            String storeCodDest,
            ReceiptLine line
    ) {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = operationCod;
        operation.ItemNumber = line.itemNumber;
        operation.SourceTable = sourceTable;
        operation.MovementEvent = TransferConstants.KARDEX_ZONE_EVENT_RECEIPT;
        operation.ProductCod = line.productCod;
        operation.Variant = line.variant;
        operation.StoreCod = storeCodDest;
        operation.WarehouseCod = line.warehouseCod;
        operation.LotNumber = line.lotNumber;
        operation.ExpirationDate = line.expirationDate;
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, line.quantity)
        );
        return operation;
    }

    private KardexEntity buildKardex(
            KardexEntity lastMovement,
            String operationCod,
            String storeCodDest,
            ReceiptLine line,
            String userCod
    ) {
        int stockBefore = lastMovement == null ? 0 : lastMovement.NumStockAfter;
        KardexEntity kardex = new KardexEntity();
        kardex.OperationCod = operationCod;
        kardex.ItemNumber = line.itemNumber;
        kardex.SourceTable = TransferConstants.KARDEX_SOURCE_TABLE;
        kardex.TypeOperation = TransferConstants.KARDEX_TYPE_IN;
        kardex.ProductCod = line.productCod;
        kardex.Variant = line.variant;
        kardex.StoreCod = storeCodDest;
        kardex.WarehouseCod = line.warehouseCod;
        kardex.NumStockBefore = stockBefore;
        kardex.NumStockMoved = line.quantity;
        kardex.NumStockAfter = stockBefore + line.quantity;
        kardex.LotNumber = line.lotNumber;
        kardex.ExpirationDate = line.expirationDate;
        kardex.TypeOperationCod = 6;
        kardex.session(userCod);
        return kardex;
    }

    private String stockKey(String storeCodDest, ReceiptLine line) {
        return line.productCod + "|" + line.variant + "|"
                + storeCodDest + "|" + line.warehouseCod;
    }

    private record ReceiptLine(
            int itemNumber,
            String productCod,
            String variant,
            String warehouseCod,
            int quantity,
            String lotNumber,
            Date expirationDate
    ) {
    }
}
