package com.ccadmin.app.product.model.entity.id;

import java.io.Serializable;

public class ProductSearchID implements Serializable {

    public String ProductCod;
    public String StoreCod;

    public ProductSearchID()
    {

    }

    public ProductSearchID(String ProductCod,String StoreCod)
    {
        this.ProductCod = ProductCod;
        this.StoreCod = StoreCod;
    }

}
