package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.BrandEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandRepository extends JpaRepository<BrandEntity,String>, CcAdminRepository<BrandEntity,String> {

    @Query(value = """
            select * from brand b where b.Status = 'A'
            """,nativeQuery = true)
    public List<BrandEntity> findAllActive();

    @Override
    @Query(value = """
            select count(1) from brand b
            where
            b.BrandCod = :id
            or b.BrandName like %:query%
            """,nativeQuery = true)
    public int countByQueryText(
             @Param("id") String id
            ,@Param("query") String query
    );

    @Override
    @Query(value = """
            select * from brand b
            where
            b.BrandCod = :id
            or b.BrandName like %:query%
            limit :init,:limit
            """,nativeQuery = true)
    public List<BrandEntity> findByQueryText(
             @Param("id") String id
            ,@Param("query") String query
            ,@Param("init") int init
            ,@Param("limit") int limit
    );
}
