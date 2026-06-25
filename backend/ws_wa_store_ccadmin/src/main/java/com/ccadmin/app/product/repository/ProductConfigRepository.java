package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductConfigRepository extends JpaRepository<ProductConfigEntity,String> {
}
