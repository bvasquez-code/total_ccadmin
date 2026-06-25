package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductInfoWarehouseRepository  extends JpaRepository<ProductInfoWarehouseEntity, ProductInfoWarehouseId> {

    @Query( value = """
            select * from product_info_warehouse T
            where T.WarehouseCod in ( select W.WarehouseCod  from warehouse W where W.StoreCod = :StoreCod )
            and T.ProductCod = :ProductCod
            """, nativeQuery = true)
    public List<ProductInfoWarehouseEntity> findInfoWarehouse(String StoreCod,String ProductCod);

    @Modifying
    @Query( value = """
             INSERT product_info_warehouse
             	(ProductCod , Variant , WarehouseCod , NumDigitalStock , NumPhysicalStock , CreationUser , CreationDate )
             SELECT
             	PV.ProductCod , PV.Variant, WH.WarehouseCod , 0 ,0 ,  PR.CreationUser , NOW()
             FROM
             	product PR, product_variant PV, warehouse WH
             WHERE
             	 PR.ProductCod = PV.ProductCod
                 AND PR.ProductCod = :ProductCod
            """, nativeQuery = true)
    public void saveAllInfo(@Param("ProductCod") String ProductCod);

    @Query( value = """
            select piw.* from product_info_warehouse piw
            where piw.WarehouseCod in ( select w.WarehouseCod from warehouse w where w.StoreCod = :StoreCod )
            """, nativeQuery = true)
    public List<ProductInfoWarehouseEntity> findByStoreCod(String StoreCod);

    @Modifying
    @Query( value = """
             INSERT product_info_warehouse
             	(ProductCod , Variant , WarehouseCod , NumDigitalStock , NumPhysicalStock , CreationUser , CreationDate )
             SELECT
             	PV.ProductCod , PV.Variant, WH.WarehouseCod , 0 ,0 ,  PR.CreationUser , NOW()
             FROM
             	product PR, product_variant PV, warehouse WH
             WHERE
             	 PR.ProductCod = PV.ProductCod
                 AND PR.ProductCod IN :ProductCodList
            """, nativeQuery = true)
    public void saveAllInfo(@Param("ProductCodList") List<String> ProductCodList);
}
