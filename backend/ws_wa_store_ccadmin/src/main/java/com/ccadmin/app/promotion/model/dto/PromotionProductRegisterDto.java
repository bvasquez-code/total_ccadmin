package com.ccadmin.app.promotion.model.dto;

import com.ccadmin.app.promotion.model.entity.PromotionEntity;
import com.ccadmin.app.promotion.model.entity.PromotionProductEntity;
import com.ccadmin.app.promotion.model.entity.PromotionStoreEntity;

import java.util.List;

public class PromotionProductRegisterDto {

    public PromotionEntity Promotion;
    public List<PromotionProductEntity> ProductList;
    public List<PromotionStoreEntity> StoreList;
}
