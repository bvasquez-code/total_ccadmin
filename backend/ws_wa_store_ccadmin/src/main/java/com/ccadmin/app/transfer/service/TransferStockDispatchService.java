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
public class TransferStockDispatchService {

    @Autowired
    private KardexZoneShared kardexZoneShared;
    @Autowired
    private KardexShared kardexShared;

    @Transactional(rollbackOn = Exception.class)
    public List<KardexEntity> dispatchTransfer(
            String transferCod,
            String storeCodOrigin,
            List<TransferDetEntity> detailList,
            String userCod
    ) throws TransferException {
        List<DispatchLine> lineList = detailList.stream()
                .filter(detail -> detail.NumUnitDispatch > 0)
                .map(detail -> new DispatchLine(
                        detail.ItemNumber,
                        detail.ProductCod,
                        detail.Variant,
                        detail.WarehouseCodOrigin,
                        detail.NumUnitDispatch,
                        detail.LotNumber,
                        detail.ExpirationDate
                ))
                .toList();
        return this.dispatch(
                transferCod,
                TransferConstants.KARDEX_SOURCE_TABLE,
                storeCodOrigin,
                lineList,
                userCod
        );
    }

    @Transactional(rollbackOn = Exception.class)
    public List<KardexEntity> dispatchTransferRequest(
            String transferReqCod,
            String storeCodOrigin,
            List<TransferRequestDetEntity> detailList,
            String userCod
    ) throws TransferException {
        List<DispatchLine> lineList = detailList.stream()
                .filter(detail -> detail.NumUnit > 0)
                .map(detail -> new DispatchLine(
                        detail.ItemNumber,
                        detail.ProductCod,
                        detail.Variant,
                        detail.WarehouseCodOrigin,
                        detail.NumUnit,
                        detail.LotNumber,
                        detail.ExpirationDate
                ))
                .toList();
        return this.dispatch(
                transferReqCod,
                TransferConstants.KARDEX_ZONE_SOURCE_REQUEST,
                storeCodOrigin,
                lineList,
                userCod
        );
    }

    private List<KardexEntity> dispatch(
            String operationCod,
            String sourceTable,
            String storeCodOrigin,
            List<DispatchLine> lineList,
            String userCod
    ) throws TransferException {
        if (operationCod == null || operationCod.isBlank()) {
            throw new TransferException("El codigo de transferencia es obligatorio");
        }
        if (lineList == null || lineList.isEmpty()) {
            throw new TransferException("La transferencia no tiene unidades para despachar");
        }

        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (DispatchLine line : lineList) {
            if (line.quantity <= 0) {
                throw new TransferException("La cantidad a despachar debe ser mayor que cero");
            }
            if (this.kardexZoneShared.apply(
                    this.buildDispatch(operationCod, sourceTable, storeCodOrigin, line),
                    userCod
            ).isEmpty()) {
                continue;
            }

            String stockKey = this.stockKey(storeCodOrigin, line);
            KardexEntity lastMovement = lastMovementByStock.computeIfAbsent(
                    stockKey,
                    ignored -> this.kardexShared.findLastMovement(
                            line.productCod,
                            line.variant,
                            line.warehouseCod,
                            storeCodOrigin
                    )
            );
            KardexEntity kardex = this.buildKardex(
                    lastMovement,
                    operationCod,
                    storeCodOrigin,
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

    private KardexZoneOperationDto buildDispatch(
            String operationCod,
            String sourceTable,
            String storeCodOrigin,
            DispatchLine line
    ) {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = operationCod;
        operation.ItemNumber = line.itemNumber;
        operation.SourceTable = sourceTable;
        operation.MovementEvent = TransferConstants.KARDEX_ZONE_EVENT_DISPATCH;
        operation.ProductCod = line.productCod;
        operation.Variant = line.variant;
        operation.StoreCod = storeCodOrigin;
        operation.WarehouseCod = line.warehouseCod;
        operation.LotNumber = line.lotNumber;
        operation.ExpirationDate = line.expirationDate;
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, line.quantity * -1)
        );
        return operation;
    }

    private KardexEntity buildKardex(
            KardexEntity lastMovement,
            String operationCod,
            String storeCodOrigin,
            DispatchLine line,
            String userCod
    ) throws TransferException {
        int stockBefore = lastMovement == null ? 0 : lastMovement.NumStockAfter;
        if (stockBefore < line.quantity) {
            throw new TransferException("Stock total insuficiente para el producto " + line.productCod);
        }

        KardexEntity kardex = new KardexEntity();
        kardex.OperationCod = operationCod;
        kardex.ItemNumber = line.itemNumber;
        kardex.SourceTable = TransferConstants.KARDEX_SOURCE_TABLE;
        kardex.TypeOperation = TransferConstants.KARDEX_TYPE_OUT;
        kardex.ProductCod = line.productCod;
        kardex.Variant = line.variant;
        kardex.StoreCod = storeCodOrigin;
        kardex.WarehouseCod = line.warehouseCod;
        kardex.NumStockBefore = stockBefore;
        kardex.NumStockMoved = line.quantity;
        kardex.NumStockAfter = stockBefore - line.quantity;
        kardex.LotNumber = line.lotNumber;
        kardex.ExpirationDate = line.expirationDate;
        kardex.TypeOperationCod = 5;
        kardex.session(userCod);
        return kardex;
    }

    private String stockKey(String storeCodOrigin, DispatchLine line) {
        return line.productCod + "|" + line.variant + "|"
                + storeCodOrigin + "|" + line.warehouseCod;
    }

    private record DispatchLine(
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
