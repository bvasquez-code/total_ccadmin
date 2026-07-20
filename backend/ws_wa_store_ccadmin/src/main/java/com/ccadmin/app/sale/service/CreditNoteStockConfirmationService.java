package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
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
public class CreditNoteStockConfirmationService {

    @Autowired
    private KardexZoneShared kardexZoneShared;
    @Autowired
    private KardexShared kardexShared;

    @Transactional(rollbackOn = Exception.class)
    public List<KardexEntity> addUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.validate(creditNoteHead, detailList, warehouse);

        List<KardexZoneEntity> kardexZoneList = this.createKardexZoneList(
                creditNoteHead, detailList, warehouse, userCod
        );
        List<KardexZoneEntity> savedKardexZoneList = this.kardexZoneShared.saveAll(kardexZoneList);
        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (CreditNoteDetEntity detail : detailList) {
            if (!this.wasSaved(savedKardexZoneList, detail.ItemNumber)) {
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
                    detail.NumUnit,
                    KardexZoneConstants.TYPE_OPERATION_ADD
            ).session(userCod);
            kardexList.add(kardex);
            lastMovementByStock.put(stockKey, kardex);
        }

        if (!kardexList.isEmpty()) {
            this.kardexShared.saveAllLedgerOnly(kardexList);
        }
        return kardexList;
    }

    private List<KardexZoneEntity> createKardexZoneList(
            CreditNoteHeadEntity head, List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse, String userCod
    ) {
        List<KardexZoneEntity> result = new ArrayList<>();
        Map<String, ProductInfoWarehouseEntity> stockCursorByWarehouse = new HashMap<>();
        for (CreditNoteDetEntity detail : detailList) {
            if (this.kardexZoneShared.isApplied(
                    SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                    head.CreditNoteCod, detail.ItemNumber,
                    SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
            )) {
                continue;
            }
            String key = this.stockKey(head, detail, warehouse);
            ProductInfoWarehouseEntity stockCursor = stockCursorByWarehouse.computeIfAbsent(
                    key,
                    ignored -> this.kardexZoneShared.findStockForUpdate(
                            detail.ProductCod, detail.Variant,
                            head.StoreCod, warehouse.WarehouseCod
                    )
            );
            result.addAll(KardexZoneEntity.buildCreditNoteConfirmation(
                    head, detail, warehouse, stockCursor, userCod
            ));
        }
        return result;
    }

    private boolean wasSaved(List<KardexZoneEntity> movementList, int itemNumber) {
        return movementList.stream().anyMatch(movement ->
                movement.ItemNumber == itemNumber
                        && SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION
                        .equals(movement.MovementEvent)
        );
    }

    private void validate(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse
    ) throws SaleException {
        if (creditNoteHead == null || creditNoteHead.CreditNoteCod == null
                || creditNoteHead.CreditNoteCod.isBlank()) {
            throw new SaleException("La nota de credito es obligatoria para ingresar stock");
        }
        if (warehouse == null || warehouse.WarehouseCod == null || warehouse.WarehouseCod.isBlank()) {
            throw new SaleException("El almacen de la nota de credito es obligatorio");
        }
        if (detailList == null || detailList.isEmpty()) {
            throw new SaleException("La nota de credito no tiene detalle para ingresar stock");
        }
        for (CreditNoteDetEntity detail : detailList) {
            if (detail == null || detail.NumUnit == null || detail.NumUnit <= 0) {
                throw new SaleException("La cantidad de la nota de credito debe ser mayor que cero");
            }
            if (!creditNoteHead.CreditNoteCod.equals(detail.CreditNoteCod)) {
                throw new SaleException("El detalle no corresponde a la nota de credito");
            }
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
