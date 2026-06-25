package com.ccadmin.app.supplier.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.supplier.model.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<SupplierEntity,String>, CcAdminRepository<SupplierEntity,String> {

    @Query(value = """
            select * from supplier s where s.PersonCod = :PersonCod and s.Status = 'A'
            """,nativeQuery = true)
    public SupplierEntity findByPersonCod(@Param("PersonCod") String PersonCod);

    @Override
    @Query(value = """
            SELECT COUNT(1) FROM supplier s
            INNER JOIN person p ON p.PersonCod = s.PersonCod
            where
            s.PersonCod = :id or CONCAT(p.Names,' ',p.LastNames) like %:query%
            and p.Status = 'A'
            and s.Status = 'A'
            """,nativeQuery = true)
    public int countByQueryText(
              @Param("id") String id
            , @Param("query") String query
    );

    @Override
    @Query(value = """
            SELECT s.* FROM supplier s
            INNER JOIN person p ON p.PersonCod = s.PersonCod
            where
            s.PersonCod = :id or CONCAT(p.Names,' ',p.LastNames) like %:query%
            and p.Status = 'A'
            and s.Status = 'A'
            order by s.ModifyDate desc
            limit :init,:limit
            """,nativeQuery = true)
    public List<SupplierEntity> findByQueryText(
              @Param("id") String id
            , @Param("query") String query
            , @Param("init") int init
            , @Param("limit") int limit
    );
}
