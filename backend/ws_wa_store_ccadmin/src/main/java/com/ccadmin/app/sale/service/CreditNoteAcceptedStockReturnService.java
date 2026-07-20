package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
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
public class CreditNoteAcceptedStockReturnService {

    @Autowired
    private KardexZoneShared kardexZoneShared;

    @Transactional(rollbackOn = Exception.class)
    public void moveAcceptedStockToPhysical(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.validate(creditNoteHead, detailList, warehouse);

        List<KardexZoneEntity> kardexZoneList = new ArrayList<>();
        Map<String, ProductInfoWarehouseEntity> stockCursorByWarehouse = new HashMap<>();
        for (CreditNoteDetEntity detail : detailList) {
            int returned = detail.NumUnitStockReturned == null ? 0 : detail.NumUnitStockReturned;
            if (returned < 0 || returned > detail.NumUnit) {
                throw new SaleException("Cantidad de retorno invalida para el producto " + detail.ProductCod);
            }
            if (returned == 0) {
                continue;
            }

            this.validateUnavailableStockOrigin(creditNoteHead, detail, warehouse);
            if (this.kardexZoneShared.isApplied(
                    SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE,
                    creditNoteHead.CreditNoteCod, detail.ItemNumber,
                    SaleConstants.KARDEX_ZONE_EVENT_CREDIT_NOTE_ACCEPTED_RETURN
            )) {
                continue;
            }
            String key = detail.ProductCod + "|" + detail.Variant + "|"
                    + creditNoteHead.StoreCod + "|" + warehouse.WarehouseCod;
            ProductInfoWarehouseEntity stockCursor = stockCursorByWarehouse.computeIfAbsent(
                    key,
                    ignored -> this.kardexZoneShared.findStockForUpdate(
                            detail.ProductCod, detail.Variant,
                            creditNoteHead.StoreCod, warehouse.WarehouseCod
                    )
            );
            kardexZoneList.addAll(KardexZoneEntity.buildCreditNoteAcceptedReturn(
                    creditNoteHead, detail, warehouse, returned, stockCursor, userCod
            ));
        }
        this.kardexZoneShared.saveAll(kardexZoneList);
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

    private void validate(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse
    ) throws SaleException {
        if (creditNoteHead == null || creditNoteHead.CreditNoteCod == null
                || creditNoteHead.CreditNoteCod.isBlank()) {
            throw new SaleException("La nota de credito es obligatoria para retornar stock");
        }
        if (warehouse == null || warehouse.WarehouseCod == null || warehouse.WarehouseCod.isBlank()) {
            throw new SaleException("El almacen de la nota de credito es obligatorio");
        }
        if (detailList == null || detailList.isEmpty()) {
            throw new SaleException("La nota de credito no tiene detalle para retornar stock");
        }
    }
}
