package com.ccadmin.app.product.model.entity.id;

import java.io.Serializable;

public class ProductPictureID implements Serializable {

    public String ProductCod;
    public String FileCod;

    public ProductPictureID(){

    }

    public ProductPictureID(String productCod, String fileCod) {
        ProductCod = productCod;
        FileCod = fileCod;
    }
}
