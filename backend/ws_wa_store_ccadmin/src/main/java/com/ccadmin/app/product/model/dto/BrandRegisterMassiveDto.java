package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.BrandEntity;
import java.util.ArrayList;
import java.util.List;

public class BrandRegisterMassiveDto {

    public List<BrandEntity> brandList;

    public BrandRegisterMassiveDto() {
        this.brandList = new ArrayList<>();
    }
}
