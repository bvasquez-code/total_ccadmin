package com.ccadmin.app.promotion.repository;

import com.ccadmin.app.promotion.model.entity.PromotionStoreEntity;
import com.ccadmin.app.promotion.model.entity.id.PromotionStoreID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionStoreRepository extends JpaRepository<PromotionStoreEntity, PromotionStoreID> {
}
