package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductConfigRepository extends JpaRepository<ProductConfigEntity, ProductConfigID> {

    @Query(value = """
            select pc.* from product_config pc
            where pc.ProductCod = :ProductCod
            """, nativeQuery = true)
    List<ProductConfigEntity> findByProductCod(@Param("ProductCod") String ProductCod);

    @Query(value = """
            select pc.* from product_config pc
            where pc.ProductCod = :ProductCod
            limit 1
            """, nativeQuery = true)
    ProductConfigEntity findAnyByProductCod(@Param("ProductCod") String ProductCod);
}
