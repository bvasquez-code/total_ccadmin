package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreditNoteRejectedStockExitService {

    @Autowired
    private KardexZoneShared kardexZoneShared;
    @Autowired
    private KardexShared kardexShared;

    @Transactional(rollbackOn = Exception.class)
    public List<KardexEntity> removeRejectedStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.validate(creditNoteHead, detailList, warehouse);

        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (CreditNoteDetEntity detail : detailList) {
            int returned = detail.NumUnitStockReturned == null ? 0 : detail.NumUnitStockReturned;
            if (returned < 0 || returned > detail.NumUnit) {
                throw new SaleException("Cantidad de retorno invalida para el producto " + detail.ProductCod);
            }

            int rejected = detail.NumUnit - returned;
            if (rejected == 0) {
                continue;
            }

            this.validateUnavailableStockOrigin(creditNoteHead, detail, warehouse);
            if (this.kardexZoneShared.apply(
                    this.buildRejectedExit(creditNoteHead, detail, warehouse, rejected),
                    userCod
            ).isEmpty()) {
                continue;
            }

            String stockKey = this.stockKey(creditNoteHead, detail, warehouse);
            KardexEntity lastMovement = lastMovementByStock.computeIfAbsent(
                    stockKey,
                    ignored -> this.kardexShared.findLastMovement(
                            detail.ProductCod,
                            detail.Variant,
                            warehouse.WarehouseCod,
                            creditNoteHead.StoreCod
                    )
            );
            KardexEntity kardex = new KardexEntity(
                    lastMovement,
                    detail,
                    creditNoteHead.StoreCod,
                    warehouse.WarehouseCod,
                    rejected,
                    KardexZoneConstants.TYPE_OPERATION_SUBTRACT
            ).session(userCod);
            kardexList.add(kardex);
            lastMovementByStock.put(stockKey, kardex);
        }

        if (!kardexList.isEmpty()) {
            this.kardexShared.saveAllLedgerOnly(kardexList);
        }
        return kardexList;
    }

    private void validateUnavailableStockOrigin(
            CreditNoteHeadEntity creditNoteHead,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse
    ) throws SaleException {
        List<KardexZoneEntity> confirmationList = this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                creditNoteHead.CreditNoteCod,
                detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
        );

        boolean unavailableWasAdded = confirmationList.stream().anyMatch(movement ->
                KardexZoneConstants.ZONE_UNAVAILABLE.equals(movement.ZoneStockMoved)
                        && KardexZoneConstants.TYPE_OPERATION_ADD.equals(movement.TypeOperation)
                        && detail.NumUnit == movement.NumStockMoved
                        && detail.ProductCod.equals(movement.ProductCod)
                        && detail.Variant.equals(movement.Variant)
                        && warehouse.WarehouseCod.equals(movement.WarehouseCod)
        );

        boolean historicalBaselineExists = !unavailableWasAdded
                && this.kardexZoneShared.hasLegacyUnavailableBaseline(
                        detail.ProductCod,
                        detail.Variant,
                        creditNoteHead.StoreCod,
                        warehouse.WarehouseCod,
                        detail.NumUnit,
                        creditNoteHead.CreationDate
                );

        if (!unavailableWasAdded && !historicalBaselineExists) {
            throw new SaleException(
                    "No existe stock no disponible confirmado para el item " + detail.ItemNumber
                            + " de la nota de credito " + creditNoteHead.CreditNoteCod
            );
        }
    }

    private KardexZoneOperationDto buildRejectedExit(
            CreditNoteHeadEntity creditNoteHead,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse,
            int rejected
    ) {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = creditNoteHead.CreditNoteCod;
        operation.ItemNumber = detail.ItemNumber;
        operation.SourceTable = SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE;
        operation.MovementEvent = SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_REJECTED_STOCK_EXIT;
        operation.ProductCod = detail.ProductCod;
        operation.Variant = detail.Variant;
        operation.StoreCod = creditNoteHead.StoreCod;
        operation.WarehouseCod = warehouse.WarehouseCod;
        operation.LotNumber = detail.LotNumber;
        operation.ExpirationDate = detail.ExpirationDate;
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_UNAVAILABLE, rejected * -1)
        );
        return operation;
    }

    private void validate(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse
    ) throws SaleException {
        if (creditNoteHead == null || creditNoteHead.CreditNoteCod == null
                || creditNoteHead.CreditNoteCod.isBlank()) {
            throw new SaleException("La nota de credito es obligatoria para retirar stock rechazado");
        }
        if (warehouse == null || warehouse.WarehouseCod == null || warehouse.WarehouseCod.isBlank()) {
            throw new SaleException("El almacen de la nota de credito es obligatorio");
        }
        if (detailList == null || detailList.isEmpty()) {
            throw new SaleException("La nota de credito no tiene detalle para retirar stock rechazado");
        }
    }

    private String stockKey(
            CreditNoteHeadEntity creditNoteHead,
            CreditNoteDetEntity detail,
            WarehouseEntity warehouse
    ) {
        return detail.ProductCod + "|" + detail.Variant + "|"
                + creditNoteHead.StoreCod + "|" + warehouse.WarehouseCod;
    }
}
