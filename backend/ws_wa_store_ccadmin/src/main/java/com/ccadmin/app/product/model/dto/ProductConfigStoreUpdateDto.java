package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;

import java.util.ArrayList;
import java.util.List;

public class ProductConfigStoreUpdateDto {
    public String ProductCod;
    public String StoreCod;
    public List<String> StoreCodList = new ArrayList<>();
    public boolean ApplyAllStores;
    public ProductConfigEntity config = new ProductConfigEntity();
}
