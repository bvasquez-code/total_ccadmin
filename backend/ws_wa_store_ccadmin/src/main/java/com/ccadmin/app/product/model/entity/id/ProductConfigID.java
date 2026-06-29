package com.ccadmin.app.product.model.entity.id;

import java.io.Serializable;

public class ProductConfigID implements Serializable {
    public String ProductCod;
    public String StoreCod;

    public ProductConfigID() {
    }

    public ProductConfigID(String productCod, String storeCod) {
        ProductCod = productCod;
        StoreCod = storeCod;
    }
}
