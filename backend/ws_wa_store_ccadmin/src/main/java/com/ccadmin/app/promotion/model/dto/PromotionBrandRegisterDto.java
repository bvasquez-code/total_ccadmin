package com.ccadmin.app.promotion.model.dto;

import com.ccadmin.app.promotion.model.entity.PromotionEntity;
import com.ccadmin.app.promotion.model.entity.PromotionStoreEntity;

import java.util.List;

public class PromotionBrandRegisterDto {

    public PromotionEntity Promotion;
    public List<PromotionBrandDto> BrandList;
    public List<PromotionStoreEntity> StoreList;
}
