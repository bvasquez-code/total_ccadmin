package com.ccadmin.app.promotion.repository;

import com.ccadmin.app.promotion.model.entity.PromotionProductEntity;
import com.ccadmin.app.promotion.model.entity.id.PromotionProductID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProductEntity, PromotionProductID> {
}
