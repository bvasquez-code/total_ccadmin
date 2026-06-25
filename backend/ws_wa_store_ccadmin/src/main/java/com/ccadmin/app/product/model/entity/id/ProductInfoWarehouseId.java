package com.ccadmin.app.product.model.entity.id;

import java.io.Serializable;

public class ProductInfoWarehouseId implements Serializable {

    public String ProductCod;
    public String Variant;
    public String WarehouseCod;

    public ProductInfoWarehouseId()
    {

    }

    public ProductInfoWarehouseId(String productCod, String variant, String warehouseCod) {
        ProductCod = productCod;
        Variant = variant;
        WarehouseCod = warehouseCod;
    }
}
