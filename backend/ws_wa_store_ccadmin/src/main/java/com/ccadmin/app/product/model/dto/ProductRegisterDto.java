package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.ProductBarcodeEntity;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductPictureEntity;

import java.util.ArrayList;
import java.util.List;

public class ProductRegisterDto {

    public ProductEntity product;
    public ProductConfigEntity config;
    public List<ProductPictureEntity> pictureList;

    public ProductBarcodeEntity productBarcode;
    public ProductRegisterDto(){
        this.pictureList = new ArrayList<>();
    }

}
