package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "product_info")
@IdClass(ProductInfoId.class)
public class ProductInfoEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProductCod;
    @Id
    public String Variant;
    @Id
    public String StoreCod;
    public int NumDigitalStock;
    public int NumPhysicalStock;
    public int NumUnavailableStock;
    public int NumReservedStock;
    public int NumTotalStock;

    public void addStock(int NumNewStock){
        this.applyStockZoneMovement(NumNewStock, 0, 0, NumNewStock);
    }

    public void applyStockZoneMovement(int physicalDelta, int unavailableDelta, int reservedDelta, int totalDelta) {
        this.NumDigitalStock = this.NumDigitalStock + physicalDelta;
        this.NumPhysicalStock = this.NumPhysicalStock + physicalDelta;
        this.NumUnavailableStock = this.NumUnavailableStock + unavailableDelta;
        this.NumReservedStock = this.NumReservedStock + reservedDelta;
        this.NumTotalStock = this.NumTotalStock + totalDelta;
        this.validateStockBalance();
    }

    public void normalizeStockTotal() {
        this.NumTotalStock = this.NumPhysicalStock + this.NumUnavailableStock + this.NumReservedStock;
        this.NumDigitalStock = this.NumPhysicalStock;
    }

    public void validateStockBalance() {
        if (this.NumPhysicalStock < 0 || this.NumUnavailableStock < 0 || this.NumReservedStock < 0 || this.NumTotalStock < 0) {
            throw new IllegalStateException("Stock negativo no permitido para producto " + this.ProductCod);
        }
        if (this.NumPhysicalStock + this.NumUnavailableStock + this.NumReservedStock != this.NumTotalStock) {
            throw new IllegalStateException("Stock inconsistente para producto " + this.ProductCod);
        }
    }

}
