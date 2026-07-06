package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;

import java.util.ArrayList;
import java.util.List;

public class ProductTaxConfigRegisterDto {
    public String ProductCod;
    public String StoreCod;
    public List<ProductTaxConfigEntity> TaxConfigList = new ArrayList<>();
}
