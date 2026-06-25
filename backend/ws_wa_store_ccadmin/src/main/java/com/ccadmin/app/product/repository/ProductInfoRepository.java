package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductInfoRepository  extends JpaRepository<ProductInfoEntity, ProductInfoId> {

    @Query( value = """
            SELECT * FROM product_info WHERE ProductCod = :ProductCod AND StoreCod = :StoreCod
            """ , nativeQuery = true)
    public List<ProductInfoEntity> findInfoStore(String ProductCod, String StoreCod);


    @Modifying
    @Query( value = """
             INSERT INTO product_info
             ( ProductCod , Variant , StoreCod , NumDigitalStock , NumPhysicalStock , CreationUser , CreationDate , Status  )
             SELECT
               prv.ProductCod , prv.Variant ,str.StoreCod , 0 , 0 , pro.CreationUser , NOW() , 'A'
             FROM product pro, store str,product_variant prv
             WHERE prv.ProductCod = pro.ProductCod
             AND pro.ProductCod = :ProductCod
            """, nativeQuery = true)
    public void saveAllInfo(@Param("ProductCod") String ProductCod);

    @Modifying
    @Query( value = """
             INSERT INTO product_info
             ( ProductCod , Variant , StoreCod , NumDigitalStock , NumPhysicalStock , CreationUser , CreationDate , Status  )
             SELECT
               prv.ProductCod , prv.Variant ,str.StoreCod , 0 , 0 , pro.CreationUser , NOW() , 'A'
             FROM product pro, store str,product_variant prv
             WHERE prv.ProductCod = pro.ProductCod
             AND pro.ProductCod IN :ProductCodList
            """, nativeQuery = true)
    public void saveAllInfo(@Param("ProductCodList") List<String> ProductCodList);



}
