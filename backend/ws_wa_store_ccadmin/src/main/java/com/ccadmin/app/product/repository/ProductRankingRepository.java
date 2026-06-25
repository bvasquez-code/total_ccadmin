package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductRankingEntity;
import com.ccadmin.app.product.model.entity.id.ProductRankingID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRankingRepository extends JpaRepository<ProductRankingEntity, ProductRankingID> {
}