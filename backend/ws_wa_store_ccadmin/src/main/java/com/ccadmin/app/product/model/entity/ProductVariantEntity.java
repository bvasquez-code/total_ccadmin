package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.model.entity.id.ProductVariantId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "product_variant")
@IdClass(ProductVariantId.class)
public class ProductVariantEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProductCod;
    @Id
    public String Variant;
    public String VariantDesc;

    public ProductVariantEntity()
    {

    }

    public ProductVariantEntity(String ProductCod)
    {
        this.ProductCod = ProductCod;
        this.Variant = "0000";
        this.VariantDesc = "default";
    }

    public ProductVariantEntity buildNew(String ProductCod){
        this.ProductCod = ProductCod;
        this.Variant = "0000";
        this.VariantDesc = "default";
        return this;
    }
    public ProductVariantEntity buildAdd(String ProductCod,String Variant,String VariantDesc){
        this.ProductCod = ProductCod;
        this.Variant = Variant;
        this.VariantDesc = VariantDesc;
        return this;
    }

    @Override
    public ProductVariantEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

}
