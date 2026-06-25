package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductVariantEntity;
import com.ccadmin.app.product.model.entity.id.ProductVariantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, ProductVariantId> {

    @Query( value = """
            SELECT * FROM product_variant WHERE ProductCod = :ProductCod 
            """ , nativeQuery = true)
    public List<ProductVariantEntity> findAllVariantProduct(String ProductCod);

}
