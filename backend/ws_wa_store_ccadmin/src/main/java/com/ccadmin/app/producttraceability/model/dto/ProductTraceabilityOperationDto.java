package com.ccadmin.app.producttraceability.model.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductTraceabilityOperationDto(
        String sourceTable,
        String operationCode,
        String storeCode,
        String relatedOperationCode,
        Map<Integer, BigDecimal> unitCostByItem,
        Map<Integer, BigDecimal> unitSalePriceByItem,
        Map<Long, String> technicalLotByKardexId
) {
    public ProductTraceabilityOperationDto(
            String sourceTable,
            String operationCode,
            String storeCode,
            String relatedOperationCode,
            Map<Integer, BigDecimal> unitCostByItem,
            Map<Integer, BigDecimal> unitSalePriceByItem
    ) {
        this(sourceTable, operationCode, storeCode, relatedOperationCode,
                unitCostByItem, unitSalePriceByItem, Map.of());
    }

    public ProductTraceabilityOperationDto {
        unitCostByItem = unitCostByItem == null ? Map.of() : Map.copyOf(unitCostByItem);
        unitSalePriceByItem = unitSalePriceByItem == null
                ? Map.of()
                : Map.copyOf(unitSalePriceByItem);
        technicalLotByKardexId = technicalLotByKardexId == null
                ? Map.of()
                : Map.copyOf(technicalLotByKardexId);
    }

    public BigDecimal unitCost(Integer itemNumber) {
        return valueOrZero(this.unitCostByItem.get(itemNumber));
    }

    public BigDecimal unitSalePrice(Integer itemNumber) {
        return valueOrZero(this.unitSalePriceByItem.get(itemNumber));
    }

    public String technicalLot(long kardexId) {
        String technicalLot = this.technicalLotByKardexId.get(kardexId);
        if (technicalLot == null || technicalLot.isBlank()) {
            throw new IllegalStateException(
                    "No se reservo el lote tecnico para el Kardex " + kardexId
            );
        }
        return technicalLot;
    }

    public ProductTraceabilityOperationDto withTechnicalLots(
            Map<Long, String> technicalLots
    ) {
        return new ProductTraceabilityOperationDto(
                this.sourceTable,
                this.operationCode,
                this.storeCode,
                this.relatedOperationCode,
                this.unitCostByItem,
                this.unitSalePriceByItem,
                technicalLots
        );
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
