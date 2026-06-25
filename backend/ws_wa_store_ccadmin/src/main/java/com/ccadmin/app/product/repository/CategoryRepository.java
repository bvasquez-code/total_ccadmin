package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity,String>, CcAdminRepository<CategoryEntity,String> {

    @Query(value = """
            select c.* from category c where c.IsCategoryDad = 'N' and c.Status = 'A'
            """,nativeQuery = true)
    public List<CategoryEntity> findAllActiveNoDad();

    @Query(value = """
            select c.* from category c where c.IsCategoryDad = 'S' and c.Status = 'A'
            """,nativeQuery = true)
    public List<CategoryEntity> findAllActiveDad();

    @Override
    @Query(value = """
            select count(1) from category c
            where
            c.CategoryCod = :id
            or c.CategoryName like %:query%
            """,nativeQuery = true)
    public int countByQueryText(
             @Param("id") String id
            ,@Param("query") String query
    );

    @Override
    @Query(value = """
            select c.* from category c
            where
            c.CategoryCod = :id
            or c.CategoryName like %:query%
            limit :init,:limit
            """,nativeQuery = true)
    public List<CategoryEntity> findByQueryText(
              @Param("id") String id
            , @Param("query") String query
            , @Param("init") int init
            , @Param("limit") int limit
    );
}
