package com.ccadmin.app.product.model.entity.id;

import java.io.Serializable;

public class ProductRankingID implements Serializable {
    public String ProductCod;
    public String StoreCod;

    public ProductRankingID(){

    }

    public ProductRankingID(String productCod, String storeCod) {
        ProductCod = productCod;
        StoreCod = storeCod;
    }
}


