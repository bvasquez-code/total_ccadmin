package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface KardexRepository extends JpaRepository<KardexEntity,Long>, CcAdminRepository<KardexEntity,Long> {

    @Query( value = """
            select k.* from kardex k
            where
            k.ProductCod = :ProductCod and k.WarehouseCod = :WarehouseCod and k.StoreCod = :StoreCod
            and k.Variant = :Variant
            order by k.kardexID desc
            limit 1
            """,nativeQuery = true)
    public KardexEntity findLastMovement(
            @Param("ProductCod") String ProductCod,
            @Param("Variant") String Variant,
            @Param("WarehouseCod") String WarehouseCod,
            @Param("StoreCod") String StoreCod
    );

    @Override
    @Query( value = """
            select count(1) from kardex k
            where k.StoreCod = :storeCod
            and (k.ProductCod  = :id or concat(k.OperationCod,' ',k.ProductCod) like %:query%)
            """,nativeQuery = true)
    public int countByQueryTextStore(
             @Param("id") String id
            ,@Param("query") String query
            ,@Param("storeCod") String storeCod
    );

    @Override
    @Query( value = """
            select k.* from kardex k
            where k.StoreCod = :storeCod
            and (k.ProductCod  = :id or concat(k.OperationCod,' ',k.ProductCod) like %:query%)
            order by k.kardexID desc
            limit :init,:limit
            """,nativeQuery = true)
    public List<KardexEntity> findByQueryTextStore(
             @Param("id") String id
            ,@Param("query") String query
            ,@Param("storeCod") String storeCod
            ,@Param("init") int init
            ,@Param("limit") int limit
    );
}
