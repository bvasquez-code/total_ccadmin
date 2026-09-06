package com.ccadmin.app.producttraceability.model.entity;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.producttraceability.model.constants.ProductTraceabilityConstants;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Entity
@Table(name = "product_traceability")
public class ProductTraceabilityEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long ProductTraceabilityID;
    @Column(length = 20, nullable = false)
    public String TechnicalLot;
    public long KardexID;
    public int AllocationNumber;
    public Long OriginProductTraceabilityID;
    public String OperationCod;
    public Integer ItemNumber;
    public String SourceTable;
    public String TypeOperation;
    public String ProductCod;
    public String Variant;
    public String StoreCod;
    public String WarehouseCod;
    public String LotNumber;
    public Date ExpirationDate;
    public int NumUnit;
    public int NumUnitAvailable;
    public BigDecimal NumUnitPriceCost;
    public BigDecimal NumTotalPriceCost;
    public BigDecimal NumUnitPriceSale;
    public BigDecimal NumTotalPriceSale;
    public Date OperationDate;
    public String AvailabilityStatus;

    public static ProductTraceabilityEntity inbound(
            KardexEntity kardex,
            int allocationNumber,
            int quantity,
            String technicalLot,
            Long originProductTraceabilityID,
            BigDecimal unitCost,
            BigDecimal unitSalePrice,
            String userCode
    ) {
        ProductTraceabilityEntity traceability = base(
                kardex, allocationNumber, quantity, technicalLot,
                originProductTraceabilityID, unitCost, unitSalePrice, userCode
        );
        traceability.NumUnitAvailable = quantity;
        traceability.AvailabilityStatus = ProductTraceabilityConstants.AVAILABILITY_AVAILABLE;
        return traceability;
    }

    public static ProductTraceabilityEntity outbound(
            KardexEntity kardex,
            int allocationNumber,
            int quantity,
            ProductTraceabilityEntity source,
            BigDecimal unitSalePrice,
            String userCode
    ) {
        ProductTraceabilityEntity traceability = base(
                kardex, allocationNumber, quantity, source.TechnicalLot,
                source.ProductTraceabilityID, source.NumUnitPriceCost,
                unitSalePrice, userCode
        );
        traceability.NumUnitAvailable = 0;
        traceability.AvailabilityStatus =
                ProductTraceabilityConstants.AVAILABILITY_NOT_APPLICABLE;
        return traceability;
    }

    public void consume(int quantity, String userCode) {
        if (quantity <= 0 || quantity > this.NumUnitAvailable) {
            throw new IllegalArgumentException("Cantidad de trazabilidad no disponible");
        }
        this.NumUnitAvailable -= quantity;
        if (this.NumUnitAvailable == 0) {
            this.AvailabilityStatus = ProductTraceabilityConstants.AVAILABILITY_EXHAUSTED;
        }
        this.addSessionModify(userCode);
    }

    private static ProductTraceabilityEntity base(
            KardexEntity kardex,
            int allocationNumber,
            int quantity,
            String technicalLot,
            Long originProductTraceabilityID,
            BigDecimal unitCost,
            BigDecimal unitSalePrice,
            String userCode
    ) {
        ProductTraceabilityEntity traceability = new ProductTraceabilityEntity();
        traceability.TechnicalLot = technicalLot;
        traceability.KardexID = kardex.kardexID;
        traceability.AllocationNumber = allocationNumber;
        traceability.OriginProductTraceabilityID = originProductTraceabilityID;
        traceability.OperationCod = kardex.OperationCod;
        traceability.ItemNumber = kardex.ItemNumber;
        traceability.SourceTable = kardex.SourceTable;
        traceability.TypeOperation = kardex.TypeOperation;
        traceability.ProductCod = kardex.ProductCod;
        traceability.Variant = kardex.Variant;
        traceability.StoreCod = kardex.StoreCod;
        traceability.WarehouseCod = kardex.WarehouseCod;
        traceability.LotNumber = kardex.LotNumber;
        traceability.ExpirationDate = kardex.ExpirationDate;
        traceability.NumUnit = quantity;
        traceability.NumUnitPriceCost = money(unitCost);
        traceability.NumTotalPriceCost = total(traceability.NumUnitPriceCost, quantity);
        traceability.NumUnitPriceSale = money(unitSalePrice);
        traceability.NumTotalPriceSale = total(traceability.NumUnitPriceSale, quantity);
        traceability.OperationDate = kardex.CreationDate == null
                ? new Date()
                : kardex.CreationDate;
        traceability.addSession(userCode);
        return traceability;
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal total(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
