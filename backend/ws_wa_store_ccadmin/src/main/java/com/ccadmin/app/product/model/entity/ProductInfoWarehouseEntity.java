package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "product_info_warehouse")
@IdClass(ProductInfoWarehouseId.class)
public class ProductInfoWarehouseEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProductCod;
    @Id
    public String Variant;
    @Id
    public String WarehouseCod;
    public int NumDigitalStock;
    public int NumPhysicalStock;

    public void addStock(int NumNewStock) {
        this.NumDigitalStock = this.NumDigitalStock + NumNewStock;
        this.NumPhysicalStock = this.NumPhysicalStock + NumNewStock;
    }
}
