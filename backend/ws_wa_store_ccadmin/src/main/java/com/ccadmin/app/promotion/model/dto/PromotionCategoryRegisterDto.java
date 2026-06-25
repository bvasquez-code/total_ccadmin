package com.ccadmin.app.promotion.model.dto;

import com.ccadmin.app.promotion.model.entity.PromotionEntity;
import com.ccadmin.app.promotion.model.entity.PromotionStoreEntity;

import java.util.List;

public class PromotionCategoryRegisterDto {

    public PromotionEntity Promotion;
    public List<PromotionCategoryDto> CategoryList;
    public List<PromotionStoreEntity> StoreList;
}
