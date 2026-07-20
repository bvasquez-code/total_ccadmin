package com.ccadmin.app.product.service;

import com.ccadmin.app.product.exception.KardexZoneException;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KardexZoneService {

    @Autowired
    private KardexZoneRepository kardexZoneRepository;
    @Autowired
    private ProductInfoRepository productInfoRepository;
    @Autowired
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Autowired
    private ProductFindCreateShared productFindCreateShared;

    public ProductInfoWarehouseEntity findStockForUpdate(
            String productCod, String variant, String storeCod, String warehouseCod
    ) {
        this.productInfoRepository.findByIdForUpdate(productCod, variant, storeCod)
                .orElseThrow(() -> new KardexZoneException(
                        "No existe stock del producto " + productCod + " en la tienda " + storeCod
                ));
        ProductInfoWarehouseEntity stock = this.productInfoWarehouseRepository.findByIdForUpdate(
                productCod, variant, warehouseCod
        ).orElseThrow(() -> new KardexZoneException(
                "No existe stock del producto " + productCod + " en el almacen " + warehouseCod
        ));
        return this.copyStock(stock);
    }

    @Transactional(rollbackOn = Exception.class)
    public List<KardexZoneEntity> saveAll(List<KardexZoneEntity> movementList) {
        if (movementList == null || movementList.isEmpty()) {
            return List.of();
        }

        List<KardexZoneEntity> pendingList = this.filterAppliedEvents(movementList);
        if (pendingList.isEmpty()) {
            return List.of();
        }

        Map<ProductStockKey, ProductInfoEntity> productStockMap = new LinkedHashMap<>();
        Map<WarehouseStockKey, ProductInfoWarehouseEntity> warehouseStockMap = new LinkedHashMap<>();
        Map<ProductStockKey, StockDelta> productDeltaMap = new LinkedHashMap<>();
        Map<WarehouseStockKey, StockDelta> warehouseDeltaMap = new LinkedHashMap<>();

        for (KardexZoneEntity movement : pendingList) {
            this.validateMovement(movement);
            ProductStockKey productKey = new ProductStockKey(
                    movement.ProductCod, movement.Variant, movement.StoreCod
            );
            WarehouseStockKey warehouseKey = new WarehouseStockKey(
                    movement.ProductCod, movement.Variant, movement.WarehouseCod
            );
            ProductInfoEntity productStock = productStockMap.computeIfAbsent(
                    productKey,
                    key -> this.productInfoRepository.findByIdForUpdate(
                            key.productCod, key.variant, key.storeCod
                    ).orElseThrow(() -> new KardexZoneException(
                            "No existe stock del producto " + key.productCod + " en la tienda " + key.storeCod
                    ))
            );
            ProductInfoWarehouseEntity warehouseStock = warehouseStockMap.computeIfAbsent(
                    warehouseKey,
                    key -> this.productInfoWarehouseRepository.findByIdForUpdate(
                            key.productCod, key.variant, key.warehouseCod
                    ).orElseThrow(() -> new KardexZoneException(
                            "No existe stock del producto " + key.productCod + " en el almacen " + key.warehouseCod
                    ))
            );

            int currentZoneStock = this.zoneStock(warehouseStock, movement.ZoneStockMoved);
            if (currentZoneStock != movement.NumZoneStockBefore) {
                throw new KardexZoneException(
                        "El saldo anterior de la zona " + movement.ZoneStockMoved
                                + " ya no coincide para el producto " + movement.ProductCod
                );
            }

            int signedQuantity = this.signedQuantity(movement);
            StockDelta movementDelta = StockDelta.from(movement.ZoneStockMoved, signedQuantity);
            this.apply(warehouseStock, movementDelta);
            productDeltaMap.merge(productKey, movementDelta, StockDelta::add);
            warehouseDeltaMap.merge(warehouseKey, movementDelta, StockDelta::add);
            productStockMap.put(productKey, productStock);
        }

        productDeltaMap.forEach((key, delta) -> {
            ProductInfoEntity stock = productStockMap.get(key);
            this.apply(stock, delta);
            stock.addSession(this.userFor(pendingList, key), false);
            this.productInfoRepository.save(stock);
            this.productFindCreateShared.save(key.productCod, key.storeCod);
        });
        warehouseDeltaMap.forEach((key, delta) -> {
            ProductInfoWarehouseEntity stock = warehouseStockMap.get(key);
            stock.addSession(this.userFor(pendingList, key), false);
            this.productInfoWarehouseRepository.save(stock);
        });
        return this.kardexZoneRepository.saveAll(pendingList);
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

    public boolean isApplied(
            String sourceTable, String operationCod, int itemNumber, String movementEvent
    ) {
        return this.kardexZoneRepository.countByEvent(
                sourceTable, operationCod, itemNumber, movementEvent
        ) > 0;
    }

    public boolean hasLegacyUnavailableBaseline(
            String productCod,
            String variant,
            String storeCod,
            String warehouseCod,
            int requiredStock,
            Date operationCreationDate
    ) {
        if (operationCreationDate == null || requiredStock <= 0) {
            return false;
        }
        return this.kardexZoneRepository.countLegacyUnavailableBaseline(
                productCod,
                variant,
                storeCod,
                warehouseCod,
                requiredStock,
                operationCreationDate
        ) > 0;
    }

    private List<KardexZoneEntity> filterAppliedEvents(List<KardexZoneEntity> movementList) {
        Map<EventKey, List<KardexZoneEntity>> eventMap = new LinkedHashMap<>();
        movementList.forEach(movement -> eventMap.computeIfAbsent(
                new EventKey(movement.SourceTable, movement.OperationCod,
                        movement.ItemNumber, movement.MovementEvent),
                ignored -> new ArrayList<>()
        ).add(movement));

        List<KardexZoneEntity> result = new ArrayList<>();
        eventMap.forEach((event, eventMovements) -> {
            if (this.kardexZoneRepository.countByEvent(
                    event.sourceTable, event.operationCod, event.itemNumber, event.movementEvent
            ) == 0) {
                result.addAll(eventMovements);
            }
        });
        return result;
    }

    private void validateMovement(KardexZoneEntity movement) {
        if (movement == null || !KardexZoneConstants.isSupported(movement.ZoneStockMoved)) {
            throw new KardexZoneException("Zona de stock no soportada");
        }
        if (movement.NumStockMoved <= 0) {
            throw new KardexZoneException("La cantidad movida debe ser mayor que cero");
        }
        int expectedAfter = movement.NumZoneStockBefore + this.signedQuantity(movement);
        if (expectedAfter != movement.NumZoneStockAfter || expectedAfter < 0) {
            throw new KardexZoneException("El saldo del movimiento de zona no es valido");
        }
    }

    private int signedQuantity(KardexZoneEntity movement) {
        if (KardexZoneConstants.TYPE_OPERATION_ADD.equals(movement.TypeOperation)) {
            return movement.NumStockMoved;
        }
        if (KardexZoneConstants.TYPE_OPERATION_SUBTRACT.equals(movement.TypeOperation)) {
            return -movement.NumStockMoved;
        }
        throw new KardexZoneException("Tipo de operacion de zona no soportado");
    }

    private int zoneStock(ProductInfoWarehouseEntity stock, String zone) {
        return switch (zone) {
            case KardexZoneConstants.ZONE_PHYSICAL -> stock.NumPhysicalStock;
            case KardexZoneConstants.ZONE_UNAVAILABLE -> stock.NumUnavailableStock;
            case KardexZoneConstants.ZONE_RESERVED -> stock.NumReservedStock;
            default -> throw new KardexZoneException("Zona de stock no soportada");
        };
    }

    private void apply(ProductInfoWarehouseEntity stock, StockDelta delta) {
        try {
            stock.applyStockAdjustment(delta.physical, delta.unavailable, delta.reserved, delta.total());
        } catch (IllegalStateException exception) {
            throw new KardexZoneException(exception.getMessage());
        }
    }

    private void apply(ProductInfoEntity stock, StockDelta delta) {
        try {
            stock.applyStockAdjustment(delta.physical, delta.unavailable, delta.reserved, delta.total());
        } catch (IllegalStateException exception) {
            throw new KardexZoneException(exception.getMessage());
        }
    }

    private ProductInfoWarehouseEntity copyStock(ProductInfoWarehouseEntity source) {
        ProductInfoWarehouseEntity result = new ProductInfoWarehouseEntity();
        result.ProductCod = source.ProductCod;
        result.Variant = source.Variant;
        result.WarehouseCod = source.WarehouseCod;
        result.NumPhysicalStock = source.NumPhysicalStock;
        result.NumDigitalStock = source.NumDigitalStock;
        result.NumUnavailableStock = source.NumUnavailableStock;
        result.NumReservedStock = source.NumReservedStock;
        result.NumTotalStock = source.NumTotalStock;
        return result;
    }

    private String userFor(List<KardexZoneEntity> movements, ProductStockKey key) {
        return movements.stream()
                .filter(item -> key.productCod.equals(item.ProductCod)
                        && key.variant.equals(item.Variant)
                        && key.storeCod.equals(item.StoreCod))
                .map(item -> item.CreationUser)
                .filter(user -> user != null && !user.isBlank())
                .findFirst()
                .orElseThrow(() -> new KardexZoneException("CreationUser es obligatorio"));
    }

    private String userFor(List<KardexZoneEntity> movements, WarehouseStockKey key) {
        return movements.stream()
                .filter(item -> key.productCod.equals(item.ProductCod)
                        && key.variant.equals(item.Variant)
                        && key.warehouseCod.equals(item.WarehouseCod))
                .map(item -> item.CreationUser)
                .filter(user -> user != null && !user.isBlank())
                .findFirst()
                .orElseThrow(() -> new KardexZoneException("CreationUser es obligatorio"));
    }

    private record EventKey(String sourceTable, String operationCod, int itemNumber, String movementEvent) {
    }

    private record ProductStockKey(String productCod, String variant, String storeCod) {
    }

    private record WarehouseStockKey(String productCod, String variant, String warehouseCod) {
    }

    private record StockDelta(int physical, int unavailable, int reserved) {
        static StockDelta from(String zone, int quantity) {
            return switch (zone) {
                case KardexZoneConstants.ZONE_PHYSICAL -> new StockDelta(quantity, 0, 0);
                case KardexZoneConstants.ZONE_UNAVAILABLE -> new StockDelta(0, quantity, 0);
                case KardexZoneConstants.ZONE_RESERVED -> new StockDelta(0, 0, quantity);
                default -> throw new KardexZoneException("Zona de stock no soportada");
            };
        }

        StockDelta add(StockDelta other) {
            return new StockDelta(
                    this.physical + other.physical,
                    this.unavailable + other.unavailable,
                    this.reserved + other.reserved
            );
        }

        int total() {
            return this.physical + this.unavailable + this.reserved;
        }
    }

}
