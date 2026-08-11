package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.product.model.entity.ProductPictureEntity;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;

import java.util.ArrayList;
import java.util.List;

public class ProductDeliveryDetailDto {

    public ProductSearchEntity Product;
    public List<ProductPictureEntity> PictureList;

    public ProductDeliveryDetailDto() {
        this.PictureList = new ArrayList<>();
    }
}
