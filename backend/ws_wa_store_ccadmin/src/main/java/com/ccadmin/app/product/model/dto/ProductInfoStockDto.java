package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;

public class ProductInfoStockDto {

    public ProductInfoEntity productInfo;
    public ProductEntity product;

    public ProductInfoStockDto(ProductInfoEntity productInfo, ProductEntity product) {
        this.productInfo = productInfo;
        this.product = product;
    }
}
