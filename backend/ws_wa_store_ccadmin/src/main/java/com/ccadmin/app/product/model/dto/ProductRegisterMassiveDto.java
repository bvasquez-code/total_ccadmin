package com.ccadmin.app.product.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ProductRegisterMassiveDto {

    public List<ProductRegisterDto> productList;

    public ProductRegisterMassiveDto(){
        this.productList = new ArrayList<>();
    }
}
