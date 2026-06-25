package com.ccadmin.app.product.model.entity.id;

import jakarta.persistence.Id;

import java.io.Serializable;

public class ProductInfoId implements Serializable {

    public String ProductCod;
    public String Variant;
    public String StoreCod;

    public ProductInfoId()
    {

    }

    public ProductInfoId(String productCod, String variant, String storeCod) {
        ProductCod = productCod;
        Variant = variant;
        StoreCod = storeCod;
    }
}
