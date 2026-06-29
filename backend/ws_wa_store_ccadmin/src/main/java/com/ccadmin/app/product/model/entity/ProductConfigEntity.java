package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table( name = "product_config")
@IdClass(ProductConfigID.class)
public class ProductConfigEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProductCod;
    @Id
    public String StoreCod;
    public BigDecimal NumPrice;
    public int NumMaxStock;
    public int NumMinStock;
    public String IsDiscontable;
    public String DiscountType;
    public BigDecimal NumDiscountMax;
    public String ProductUnitName = "NIU";
    public int ProductUnitFactor = 1;
    public String Version;

    public ProductConfigEntity(){

    }

    @Override
    public ProductConfigEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

}
