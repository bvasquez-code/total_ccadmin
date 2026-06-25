package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;

public class KardexDto {

    public KardexEntity kardex;

    public ProductEntity product;
    public Object dataTransaction;
    public BusinessConfigEntity dataTypeOperation;

}
