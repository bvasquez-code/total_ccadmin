package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.CategoryEntity;
import java.util.ArrayList;
import java.util.List;

public class CategoryRegisterMassiveDto {

    public List<CategoryEntity> categoryList;

    public CategoryRegisterMassiveDto() {
        this.categoryList = new ArrayList<>();
    }
}
