package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductInfoRepository extends JpaRepository<ProductInfoEntity, ProductInfoId>, CcAdminRepository<ProductInfoEntity, ProductInfoId> {

    @Query(value = """
            select * from product_info
            where ProductCod = :ProductCod
              and Variant = :Variant
              and StoreCod = :StoreCod
            for update
            """, nativeQuery = true)
    Optional<ProductInfoEntity> findByIdForUpdate(
            @Param("ProductCod") String productCod,
            @Param("Variant") String variant,
            @Param("StoreCod") String storeCod
    );

    @Query( value = """
            SELECT * FROM product_info WHERE ProductCod = :ProductCod AND StoreCod = :StoreCod
            """ , nativeQuery = true)
    public List<ProductInfoEntity> findInfoStore(String ProductCod, String StoreCod);

    @Query(value = """
            SELECT COUNT(1)
            FROM product_info product_info
            INNER JOIN product product
                    ON product.ProductCod = product_info.ProductCod
            WHERE (:storeCod = '' OR product_info.StoreCod = :storeCod)
              AND product_info.Status = 'A'
              AND product.Status = 'A'
              AND (
                    :query = ''
                    OR CONCAT_WS(' ',
                        product_info.ProductCod,
                        product.ProductName,
                        product_info.Variant
                    ) LIKE CONCAT('%', :query, '%')
                    OR product_info.ProductCod = :id
              )
            """, nativeQuery = true)
    @Override
    int countByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod
    );

    @Query(value = """
            SELECT product_info.*
            FROM product_info product_info
            INNER JOIN product product
                    ON product.ProductCod = product_info.ProductCod
            WHERE (:storeCod = '' OR product_info.StoreCod = :storeCod)
              AND product_info.Status = 'A'
              AND product.Status = 'A'
              AND (
                    :query = ''
                    OR CONCAT_WS(' ',
                        product_info.ProductCod,
                        product.ProductName,
                        product_info.Variant
                    ) LIKE CONCAT('%', :query, '%')
                    OR product_info.ProductCod = :id
              )
            ORDER BY product.ProductName, product_info.ProductCod, product_info.Variant
            LIMIT :init, :limit
            """, nativeQuery = true)
    @Override
    List<ProductInfoEntity> findByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("init") int init,
            @Param("limit") int limit
    );


    @Modifying
    @Query( value = """
             INSERT INTO product_info
             ( ProductCod , Variant , StoreCod , NumDigitalStock , NumPhysicalStock , NumUnavailableStock , NumReservedStock , NumTotalStock , CreationUser , CreationDate , Status  )
             SELECT
               prv.ProductCod , prv.Variant ,str.StoreCod , 0 , 0 , 0 , 0 , 0 , pro.CreationUser , NOW() , 'A'
             FROM product pro, store str,product_variant prv
             WHERE prv.ProductCod = pro.ProductCod
             AND pro.ProductCod = :ProductCod
            """, nativeQuery = true)
    public void saveAllInfo(@Param("ProductCod") String ProductCod);

    @Modifying
    @Query( value = """
             INSERT INTO product_info
             ( ProductCod , Variant , StoreCod , NumDigitalStock , NumPhysicalStock , NumUnavailableStock , NumReservedStock , NumTotalStock , CreationUser , CreationDate , Status  )
             SELECT
               prv.ProductCod , prv.Variant ,str.StoreCod , 0 , 0 , 0 , 0 , 0 , pro.CreationUser , NOW() , 'A'
             FROM product pro, store str,product_variant prv
             WHERE prv.ProductCod = pro.ProductCod
             AND pro.ProductCod IN :ProductCodList
            """, nativeQuery = true)
    public void saveAllInfo(@Param("ProductCodList") List<String> ProductCodList);



}
