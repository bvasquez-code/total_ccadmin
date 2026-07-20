package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.pucharse.exception.PucharseException;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseStockReceiptService {

    @Autowired
    private KardexZoneShared kardexZoneShared;
    @Autowired
    private KardexShared kardexShared;

    @Transactional
    public List<KardexEntity> receive(
            PucharseHeadEntity purchaseHead,
            List<PucharseDetDeliveryEntity> deliveryList,
            String userCod
    ) throws PucharseException {
        this.validate(purchaseHead, deliveryList);

        List<KardexZoneEntity> kardexZoneList = this.createKardexZoneList(
                purchaseHead, deliveryList, userCod
        );
        List<KardexZoneEntity> savedKardexZoneList = this.kardexZoneShared.saveAll(kardexZoneList);
        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (PucharseDetDeliveryEntity delivery : deliveryList) {
            if (!this.wasSaved(savedKardexZoneList, delivery.ItemNumber)) {
                continue;
            }

            String stockKey = this.stockKey(purchaseHead, delivery);
            KardexEntity kardexLast = lastMovementByStock.computeIfAbsent(
                    stockKey,
                    ignored -> this.kardexShared.findLastMovement(
                            delivery.ProductCod,
                            delivery.Variant,
                            delivery.WarehouseCod,
                            purchaseHead.StoreCod
                    )
            );
            KardexEntity kardex = new KardexEntity(kardexLast, delivery, purchaseHead.StoreCod)
                    .session(userCod);
            kardexList.add(kardex);
            lastMovementByStock.put(stockKey, kardex);
        }

        if (!kardexList.isEmpty()) {
            this.kardexShared.saveAllLedgerOnly(kardexList);
        }
        return kardexList;
    }

    private List<KardexZoneEntity> createKardexZoneList(
            PucharseHeadEntity purchaseHead,
            List<PucharseDetDeliveryEntity> deliveryList,
            String userCod
    ) {
        List<KardexZoneEntity> result = new ArrayList<>();
        Map<String, ProductInfoWarehouseEntity> stockCursorByWarehouse = new HashMap<>();
        for (PucharseDetDeliveryEntity delivery : deliveryList) {
            if (this.kardexZoneShared.isApplied(
                    PucharseConstants.KARDEX_ZONE_SOURCE,
                    purchaseHead.PucharseCod,
                    delivery.ItemNumber,
                    PucharseConstants.KARDEX_ZONE_EVENT_RECEIPT
            )) {
                continue;
            }
            String key = this.stockKey(purchaseHead, delivery);
            ProductInfoWarehouseEntity stockCursor = stockCursorByWarehouse.computeIfAbsent(
                    key,
                    ignored -> this.kardexZoneShared.findStockForUpdate(
                            delivery.ProductCod, delivery.Variant,
                            purchaseHead.StoreCod, delivery.WarehouseCod
                    )
            );
            result.addAll(KardexZoneEntity.buildPurchaseReceipt(
                    purchaseHead, delivery, stockCursor, userCod
            ));
        }
        return result;
    }

    private boolean wasSaved(List<KardexZoneEntity> movementList, int itemNumber) {
        return movementList.stream().anyMatch(movement ->
                movement.ItemNumber == itemNumber
                        && PucharseConstants.KARDEX_ZONE_EVENT_RECEIPT.equals(movement.MovementEvent)
        );
    }

    private void validate(
            PucharseHeadEntity purchaseHead,
            List<PucharseDetDeliveryEntity> deliveryList
    ) throws PucharseException {
        if (purchaseHead == null || purchaseHead.PucharseCod == null || purchaseHead.PucharseCod.isBlank()) {
            throw new PucharseException("La compra es obligatoria para recibir stock");
        }
        if (deliveryList == null || deliveryList.isEmpty()) {
            throw new PucharseException("La compra no tiene entregas para recibir");
        }
        for (PucharseDetDeliveryEntity delivery : deliveryList) {
            if (delivery == null || delivery.NumUnit <= 0) {
                throw new PucharseException("La cantidad recibida debe ser mayor que cero");
            }
            if (!purchaseHead.PucharseCod.equals(delivery.PucharseCod)) {
                throw new PucharseException("La entrega no corresponde a la compra " + purchaseHead.PucharseCod);
            }
        }
    }

    private String stockKey(PucharseHeadEntity purchaseHead, PucharseDetDeliveryEntity delivery) {
        return delivery.ProductCod + "|" + delivery.Variant + "|"
                + purchaseHead.StoreCod + "|" + delivery.WarehouseCod;
    }
}
