package com.ccadmin.app.product.service;

import com.ccadmin.app.product.exception.KardexZoneException;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.repository.ProductInfoWarehouseRepository;
import com.ccadmin.app.product.shared.ProductFindCreateShared;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KardexZoneService {

    @Autowired
    private ProductInfoRepository productInfoRepository;
    @Autowired
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Autowired
    private KardexZoneRepository kardexZoneRepository;
    @Autowired
    private ProductFindCreateShared productFindCreateShared;

    @Transactional
    public List<KardexZoneEntity> apply(KardexZoneOperationDto operation, String userCod) {
        this.validate(operation, userCod);

        ProductInfoEntity productInfo = this.productInfoRepository.findByIdForUpdate(
                operation.ProductCod,
                operation.Variant,
                operation.StoreCod
        ).orElseThrow(() -> new KardexZoneException(
                "No existe stock del producto " + operation.ProductCod + " en la tienda " + operation.StoreCod
        ));

        ProductInfoWarehouseEntity productInfoWarehouse = this.productInfoWarehouseRepository.findByIdForUpdate(
                operation.ProductCod,
                operation.Variant,
                operation.WarehouseCod
        ).orElseThrow(() -> new KardexZoneException(
                "No existe stock del producto " + operation.ProductCod + " en el almacen " + operation.WarehouseCod
        ));

        if (this.exists(operation)) {
            return List.of();
        }

        Map<String, Integer> warehouseStockByZone = this.stockByZone(productInfoWarehouse);
        Map<String, Integer> deltaByZone = this.emptyDeltaByZone();
        List<KardexZoneEntity> kardexZoneList = new ArrayList<>();

        for (KardexZoneMovementDto movement : operation.MovementList) {
            int stockBefore = warehouseStockByZone.get(movement.ZoneStockMoved);
            int stockAfter = stockBefore + movement.NumStockDelta;
            if (stockAfter < 0) {
                throw new KardexZoneException(
                        "Stock insuficiente en zona " + movement.ZoneStockMoved
                                + " para producto " + operation.ProductCod
                );
            }

            KardexZoneEntity kardexZone = this.buildMovement(
                    operation,
                    movement,
                    stockBefore,
                    stockAfter,
                    userCod
            );
            kardexZoneList.add(kardexZone);
            warehouseStockByZone.put(movement.ZoneStockMoved, stockAfter);
            deltaByZone.compute(movement.ZoneStockMoved, (zone, delta) -> delta + movement.NumStockDelta);
        }

        int physicalDelta = deltaByZone.get(KardexZoneConstants.ZONE_PHYSICAL);
        int reservedDelta = deltaByZone.get(KardexZoneConstants.ZONE_RESERVED);
        int unavailableDelta = deltaByZone.get(KardexZoneConstants.ZONE_UNAVAILABLE);
        int totalDelta = physicalDelta + reservedDelta + unavailableDelta;

        try {
            productInfo.applyStockAdjustment(physicalDelta, unavailableDelta, reservedDelta, totalDelta);
            productInfoWarehouse.applyStockAdjustment(physicalDelta, unavailableDelta, reservedDelta, totalDelta);
        } catch (IllegalStateException exception) {
            throw new KardexZoneException(exception.getMessage());
        }

        productInfo.addSession(userCod, false);
        productInfoWarehouse.addSession(userCod, false);
        this.productInfoRepository.save(productInfo);
        this.productInfoWarehouseRepository.save(productInfoWarehouse);
        this.kardexZoneRepository.saveAll(kardexZoneList);
        this.productFindCreateShared.save(operation.ProductCod, operation.StoreCod);

        return kardexZoneList;
    }

    public List<KardexZoneEntity> findByEvent(
            String sourceTable,
            String operationCod,
            int itemNumber,
            String movementEvent
    ) {
        return this.kardexZoneRepository.findByEvent(
                sourceTable,
                operationCod,
                itemNumber,
                movementEvent
        );
    }

    private boolean exists(KardexZoneOperationDto operation) {
        return this.kardexZoneRepository.countByEvent(
                operation.SourceTable,
                operation.OperationCod,
                operation.ItemNumber,
                operation.MovementEvent
        ) > 0;
    }

    private Map<String, Integer> stockByZone(ProductInfoWarehouseEntity productInfoWarehouse) {
        Map<String, Integer> stockByZone = new LinkedHashMap<>();
        stockByZone.put(KardexZoneConstants.ZONE_PHYSICAL, productInfoWarehouse.NumPhysicalStock);
        stockByZone.put(KardexZoneConstants.ZONE_RESERVED, productInfoWarehouse.NumReservedStock);
        stockByZone.put(KardexZoneConstants.ZONE_UNAVAILABLE, productInfoWarehouse.NumUnavailableStock);
        return stockByZone;
    }

    private Map<String, Integer> emptyDeltaByZone() {
        Map<String, Integer> deltaByZone = new LinkedHashMap<>();
        deltaByZone.put(KardexZoneConstants.ZONE_PHYSICAL, 0);
        deltaByZone.put(KardexZoneConstants.ZONE_RESERVED, 0);
        deltaByZone.put(KardexZoneConstants.ZONE_UNAVAILABLE, 0);
        return deltaByZone;
    }

    private KardexZoneEntity buildMovement(
            KardexZoneOperationDto operation,
            KardexZoneMovementDto movement,
            int stockBefore,
            int stockAfter,
            String userCod
    ) {
        KardexZoneEntity kardexZone = new KardexZoneEntity();
        kardexZone.OperationCod = operation.OperationCod;
        kardexZone.ItemNumber = operation.ItemNumber;
        kardexZone.SourceTable = operation.SourceTable;
        kardexZone.MovementEvent = operation.MovementEvent;
        kardexZone.ProductCod = operation.ProductCod;
        kardexZone.Variant = operation.Variant;
        kardexZone.StoreCod = operation.StoreCod;
        kardexZone.WarehouseCod = operation.WarehouseCod;
        kardexZone.ZoneStockMoved = movement.ZoneStockMoved;
        kardexZone.TypeOperation = movement.NumStockDelta > 0
                ? KardexZoneConstants.TYPE_OPERATION_ADD
                : KardexZoneConstants.TYPE_OPERATION_SUBTRACT;
        kardexZone.NumStockMoved = Math.abs(movement.NumStockDelta);
        kardexZone.NumZoneStockBefore = stockBefore;
        kardexZone.NumZoneStockAfter = stockAfter;
        kardexZone.LotNumber = operation.LotNumber;
        kardexZone.ExpirationDate = operation.ExpirationDate;
        kardexZone.addSession(userCod);
        return kardexZone;
    }

    private void validate(KardexZoneOperationDto operation, String userCod) {
        if (operation == null) {
            throw new KardexZoneException("La operacion de kardex por zona es obligatoria");
        }
        this.requireText(operation.OperationCod, "OperationCod");
        this.requireText(operation.SourceTable, "SourceTable");
        this.requireText(operation.MovementEvent, "MovementEvent");
        this.requireText(operation.ProductCod, "ProductCod");
        this.requireText(operation.Variant, "Variant");
        this.requireText(operation.StoreCod, "StoreCod");
        this.requireText(operation.WarehouseCod, "WarehouseCod");
        this.requireText(userCod, "CreationUser");
        if (operation.ItemNumber <= 0) {
            throw new KardexZoneException("ItemNumber debe ser mayor que cero");
        }
        if (operation.MovementList == null || operation.MovementList.isEmpty()) {
            throw new KardexZoneException("La operacion debe contener movimientos de zona");
        }
        for (KardexZoneMovementDto movement : operation.MovementList) {
            if (movement == null || !KardexZoneConstants.isSupported(movement.ZoneStockMoved)) {
                throw new KardexZoneException("Zona de stock no soportada");
            }
            if (movement.NumStockDelta == 0) {
                throw new KardexZoneException("La cantidad movida debe ser diferente de cero");
            }
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new KardexZoneException(field + " es obligatorio");
        }
    }
}
