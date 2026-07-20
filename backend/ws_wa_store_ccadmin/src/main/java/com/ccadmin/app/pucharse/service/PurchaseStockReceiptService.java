package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexEntity;
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

        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (PucharseDetDeliveryEntity delivery : deliveryList) {
            if (this.kardexZoneShared.apply(this.buildReceipt(purchaseHead, delivery), userCod).isEmpty()) {
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

    private KardexZoneOperationDto buildReceipt(
            PucharseHeadEntity purchaseHead,
            PucharseDetDeliveryEntity delivery
    ) {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = purchaseHead.PucharseCod;
        operation.ItemNumber = delivery.ItemNumber;
        operation.SourceTable = PucharseConstants.KARDEX_ZONE_SOURCE;
        operation.MovementEvent = PucharseConstants.KARDEX_ZONE_EVENT_RECEIPT;
        operation.ProductCod = delivery.ProductCod;
        operation.Variant = delivery.Variant;
        operation.StoreCod = purchaseHead.StoreCod;
        operation.WarehouseCod = delivery.WarehouseCod;
        operation.LotNumber = delivery.LotNumber;
        operation.ExpirationDate = delivery.ExpirationDate;
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, delivery.NumUnit)
        );
        return operation;
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
