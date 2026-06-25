package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductRepository extends JpaRepository<ProductEntity,String>, CcAdminRepository<ProductEntity,String> {
    @Override
    @Query(value = """
            select count(1) from product p
            where
            p.ProductCod = :id
            or p.ProductName like %:query%
            """,nativeQuery = true)
    public int countByQueryText(String id, String query);

    @Override
    @Query(value = """
            select * from product p
            where
            p.ProductCod = :id
            or p.ProductName like %:query%
            order by p.CreationDate desc
            limit :init,:limit
            """,nativeQuery = true)
    public List<ProductEntity> findByQueryText(String id, String query, int init, int limit);


    @Query(value = """
            select p.* from product p where p.BrandCod = :BrandCod and p.Status = 'A'
            """,nativeQuery = true)
    public List<ProductEntity> findByBrandCod(@Param("BrandCod") String BrandCod);

    @Query(value = """
            select p.* from product p where p.CategoryCod = :CategoryCod and p.Status = 'A'
            """,nativeQuery = true)
    public List<ProductEntity> findByCategoryCod(@Param("CategoryCod")String CategoryCod);
}
