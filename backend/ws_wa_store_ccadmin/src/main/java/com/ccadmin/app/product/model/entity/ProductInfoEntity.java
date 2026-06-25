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

    public void addStock(int NumNewStock){
        this.NumDigitalStock = this.NumDigitalStock + NumNewStock;
        this.NumPhysicalStock = this.NumPhysicalStock + NumNewStock;
    }

}
