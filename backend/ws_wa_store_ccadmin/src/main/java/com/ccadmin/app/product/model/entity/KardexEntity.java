package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.exception.KardexExcepcion;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@Table(name = "kardex")
public class KardexEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long kardexID;
    public String OperationCod;
    public Integer ItemNumber;
    public String SourceTable;
    public String TypeOperation;
    public String ProductCod;
    public String Variant;
    public String StoreCod;
    public String WarehouseCod;
    public int NumStockBefore;
    public int NumStockMoved;
    public int NumStockAfter;
    public String LotNumber;
    public Date ExpirationDate;
    public int TypeOperationCod;

    public KardexEntity() {

    }

    public static KardexEntity build(
            String operationCod,
            Integer itemNumber,
            String sourceTable,
            String typeOperation,
            String productCod,
            String variant,
            String storeCod,
            String warehouseCod,
            int numStockMoved,
            String lotNumber,
            Date expirationDate,
            int typeOperationCod,
            String userCod
    ) {
        KardexEntity kardex = new KardexEntity();
        kardex.OperationCod = operationCod;
        kardex.ItemNumber = itemNumber;
        kardex.SourceTable = sourceTable;
        kardex.TypeOperation = typeOperation;
        kardex.ProductCod = productCod;
        kardex.Variant = variant;
        kardex.StoreCod = storeCod;
        kardex.WarehouseCod = warehouseCod;
        kardex.NumStockMoved = numStockMoved;
        kardex.LotNumber = lotNumber;
        kardex.ExpirationDate = expirationDate;
        kardex.TypeOperationCod = typeOperationCod;
        kardex.addSession(userCod);
        return kardex;
    }

    public void applyLastMovement(KardexEntity lastMovement) {
        this.NumStockBefore = lastMovement == null ? 0 : lastMovement.NumStockAfter;
        this.NumStockAfter = this.NumStockBefore + this.signedQuantity();
        this.validateNonNegativeStock();
    }

    public int signedQuantity() {
        if ("S".equals(this.TypeOperation)) {
            return this.NumStockMoved;
        }
        if ("R".equals(this.TypeOperation)) {
            return -this.NumStockMoved;
        }
        throw new KardexExcepcion("Tipo de operacion de Kardex no soportado");
    }

    public void validateNonNegativeStock() {
        if (this.NumStockAfter < 0) {
            throw new KardexExcepcion(
                    "Stock negativo no permitido. " +
                            "ProductCod=" + this.ProductCod +
                            ", Variant=" + this.Variant +
                            ", StoreCod=" + this.StoreCod +
                            ", WarehouseCod=" + this.WarehouseCod +
                            ", NumStockBefore=" + this.NumStockBefore +
                            ", NumStockMoved=" + this.NumStockMoved +
                            ", NumStockAfter=" + this.NumStockAfter);
        }
    }
}
