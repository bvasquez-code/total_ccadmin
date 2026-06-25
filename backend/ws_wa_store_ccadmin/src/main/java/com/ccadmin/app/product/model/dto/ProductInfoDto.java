package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.*;

import java.util.ArrayList;
import java.util.List;

public class ProductInfoDto {

    public ProductEntity Product;
    public ProductConfigEntity Config;
    public List<ProductVariantEntity> VariantList;
    public List<ProductInfoEntity> InfoList;
    public List<ProductInfoWarehouseEntity> InfoWarehouseList;
    public ProductPictureEntity Picture;


    public ProductInfoDto()
    {
        this.Product = new ProductEntity();
        this.Config = new ProductConfigEntity();
        this.VariantList = new ArrayList<>();
        this.InfoList = new ArrayList<>();
        this.InfoWarehouseList = new ArrayList<>();
    }
}
