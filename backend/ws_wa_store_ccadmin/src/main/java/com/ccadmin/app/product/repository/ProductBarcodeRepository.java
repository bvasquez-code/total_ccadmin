package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductBarcodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductBarcodeRepository extends JpaRepository<ProductBarcodeEntity,String> {

    @Query(value = """
            select * from product_barcode p where p.ProductCod = :ProductCod
            """ , nativeQuery = true)
    public ProductBarcodeEntity findByProductCod(@Param("ProductCod") String ProductCod);

}
