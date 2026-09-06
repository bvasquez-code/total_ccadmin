package com.ccadmin.app.producttraceability.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.producttraceability.exception.PendingProductTraceabilityException;
import com.ccadmin.app.producttraceability.model.dto.ProductTraceabilityOperationDto;
import com.ccadmin.app.producttraceability.model.entity.ProductTraceabilityEntity;
import com.ccadmin.app.producttraceability.repository.ProductTraceabilityRepository;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductTraceabilityCreateService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final KardexRepository kardexRepository;
    private final ProductTraceabilityRepository productTraceabilityRepository;

    public ProductTraceabilityCreateService(
            KardexRepository kardexRepository,
            ProductTraceabilityRepository productTraceabilityRepository
    ) {
        this.kardexRepository = kardexRepository;
        this.productTraceabilityRepository = productTraceabilityRepository;
    }

    @Transactional(rollbackOn = Exception.class)
    public void create(ProductTraceabilityOperationDto operation) {
        List<KardexEntity> movementList = this.kardexRepository
                .findTraceabilityMovementsForUpdate(
                        operation.sourceTable(),
                        operation.operationCode(),
                        operation.storeCode()
                );
        for (KardexEntity movement : movementList) {
            if (this.productTraceabilityRepository.countByKardexId(movement.kardexID) > 0) {
                continue;
            }
            if (KardexZoneConstants.TYPE_OPERATION_ADD.equals(movement.TypeOperation)) {
                this.createInbound(operation, movement);
            } else if (KardexZoneConstants.TYPE_OPERATION_SUBTRACT.equals(movement.TypeOperation)) {
                this.createOutbound(operation, movement);
            } else {
                throw new IllegalArgumentException(
                        "Tipo de movimiento Kardex no soportado: " + movement.TypeOperation
                );
            }
        }
    }

    private void createInbound(
            ProductTraceabilityOperationDto operation,
            KardexEntity movement
    ) {
        if (this.isLinkedInbound(operation.sourceTable())) {
            this.createLinkedInbound(operation, movement);
            return;
        }

        BigDecimal unitCost = this.resolveInboundCost(operation, movement);
        ProductTraceabilityEntity traceability = ProductTraceabilityEntity.inbound(
                movement,
                1,
                movement.NumStockMoved,
                operation.technicalLot(movement.kardexID),
                null,
                unitCost,
                operation.unitSalePrice(movement.ItemNumber),
                this.userCode(movement)
        );
        this.productTraceabilityRepository.save(traceability);
    }

    private void createLinkedInbound(
            ProductTraceabilityOperationDto operation,
            KardexEntity movement
    ) {
        String sourceTable = this.linkedOutboundSourceTable(operation.sourceTable());
        String operationCode = this.linkedOutboundOperationCode(operation);
        List<ProductTraceabilityEntity> sourceAllocationList =
                this.productTraceabilityRepository.findOutboundAllocationsForUpdate(
                        sourceTable,
                        operationCode,
                        movement.ItemNumber,
                        movement.ProductCod,
                        movement.Variant
                );
        if (sourceAllocationList.isEmpty()) {
            throw new PendingProductTraceabilityException(
                    "Aun no existe la trazabilidad de origen para "
                            + operation.sourceTable() + " " + operation.operationCode()
            );
        }

        int pendingQuantity = movement.NumStockMoved;
        int allocationNumber = 1;
        for (ProductTraceabilityEntity source : sourceAllocationList) {
            long alreadyLinked = this.productTraceabilityRepository
                    .sumInboundQuantityByOrigin(source.ProductTraceabilityID);
            int availableToLink = Math.max(0, source.NumUnit - Math.toIntExact(alreadyLinked));
            int assignedQuantity = Math.min(pendingQuantity, availableToLink);
            if (assignedQuantity == 0) {
                continue;
            }
            ProductTraceabilityEntity traceability = ProductTraceabilityEntity.inbound(
                    movement,
                    allocationNumber++,
                    assignedQuantity,
                    source.TechnicalLot,
                    source.ProductTraceabilityID,
                    source.NumUnitPriceCost,
                    operation.unitSalePrice(movement.ItemNumber),
                    this.userCode(movement)
            );
            this.productTraceabilityRepository.save(traceability);
            pendingQuantity -= assignedQuantity;
            if (pendingQuantity == 0) {
                return;
            }
        }

        throw new PendingProductTraceabilityException(
                "La trazabilidad de origen aun no cubre las " + movement.NumStockMoved
                        + " unidades de " + movement.ProductCod
            );
    }

    private void createOutbound(
            ProductTraceabilityOperationDto operation,
            KardexEntity movement
    ) {
        List<ProductTraceabilityEntity> sourceList = new ArrayList<>();
        Set<Long> sourceIdSet = new HashSet<>();

        this.addDistinct(
                sourceList,
                sourceIdSet,
                this.productTraceabilityRepository.findAvailableFromOperationForUpdate(
                        operation.sourceTable(),
                        operation.operationCode(),
                        movement.ItemNumber,
                        movement.StoreCod,
                        movement.WarehouseCod,
                        movement.ProductCod,
                        movement.Variant
                )
        );
        this.addDistinct(
                sourceList,
                sourceIdSet,
                this.productTraceabilityRepository.findAvailableForUpdate(
                        movement.StoreCod,
                        movement.WarehouseCod,
                        movement.ProductCod,
                        movement.Variant,
                        null,
                        null
                )
        );

        int pendingQuantity = movement.NumStockMoved;
        int allocationNumber = 1;
        for (ProductTraceabilityEntity source : sourceList) {
            int assignedQuantity = Math.min(pendingQuantity, source.NumUnitAvailable);
            if (assignedQuantity == 0) {
                continue;
            }
            source.consume(assignedQuantity, this.userCode(movement));
            this.productTraceabilityRepository.save(source);
            this.productTraceabilityRepository.save(ProductTraceabilityEntity.outbound(
                    movement,
                    allocationNumber++,
                    assignedQuantity,
                    source,
                    this.resolveOutboundSalePrice(operation, movement),
                    this.userCode(movement)
            ));
            pendingQuantity -= assignedQuantity;
            if (pendingQuantity == 0) {
                return;
            }
        }

        throw new PendingProductTraceabilityException(
                "No hay identidad disponible para " + pendingQuantity + " de "
                        + movement.ProductCod + " en " + movement.WarehouseCod
        );
    }

    private BigDecimal resolveInboundCost(
            ProductTraceabilityOperationDto operation,
            KardexEntity movement
    ) {
        BigDecimal documentedCost = operation.unitCost(movement.ItemNumber);
        if (!StockMovementConstants.SOURCE_ENTRY.equals(operation.sourceTable())
                || documentedCost.signum() > 0) {
            return documentedCost;
        }
        return this.productTraceabilityRepository.findLastOutbound(
                        movement.ProductCod,
                        movement.Variant,
                        movement.StoreCod,
                        movement.WarehouseCod
                )
                .map(item -> item.NumUnitPriceCost)
                .or(() -> this.productTraceabilityRepository.findLastPurchaseInbound(
                        movement.ProductCod,
                        movement.Variant,
                        movement.StoreCod
                ).map(item -> item.NumUnitPriceCost))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal resolveOutboundSalePrice(
            ProductTraceabilityOperationDto operation,
            KardexEntity movement
    ) {
        if (SaleConstants.KARDEX_ZONE_SOURCE_SALE.equals(operation.sourceTable())) {
            return operation.unitSalePrice(movement.ItemNumber);
        }
        return BigDecimal.ZERO;
    }

    private boolean isLinkedInbound(String sourceTable) {
        return TransferConstants.KARDEX_SOURCE_TABLE.equals(sourceTable)
                || TransferConstants.KARDEX_ZONE_SOURCE_REQUEST.equals(sourceTable)
                || SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE.equals(sourceTable);
    }

    private String linkedOutboundSourceTable(String sourceTable) {
        if (SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE.equals(sourceTable)) {
            return SaleConstants.KARDEX_ZONE_SOURCE_SALE;
        }
        return sourceTable;
    }

    private String linkedOutboundOperationCode(ProductTraceabilityOperationDto operation) {
        if (SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE.equals(operation.sourceTable())) {
            if (operation.relatedOperationCode() == null
                    || operation.relatedOperationCode().isBlank()) {
                throw new IllegalArgumentException(
                        "La nota de credito no tiene una venta de origen"
                );
            }
            return operation.relatedOperationCode();
        }
        return operation.operationCode();
    }

    private void addDistinct(
            List<ProductTraceabilityEntity> destination,
            Set<Long> idSet,
            List<ProductTraceabilityEntity> source
    ) {
        source.forEach(item -> {
            if (idSet.add(item.ProductTraceabilityID)) {
                destination.add(item);
            }
        });
    }

    private String userCode(KardexEntity movement) {
        return movement.CreationUser == null || movement.CreationUser.isBlank()
                ? SYSTEM_USER
                : movement.CreationUser;
    }
}
