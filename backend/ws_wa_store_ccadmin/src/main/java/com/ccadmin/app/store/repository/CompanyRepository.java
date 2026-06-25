package com.ccadmin.app.store.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyRepository extends JpaRepository<CompanyEntity, String>,
        CcAdminRepository<CompanyEntity, String> {

    @Override
    @Query(value = """
            select count(1)
            from company c
            where c.CompanyCod = :id
               or c.TaxId = :query
               or c.LegalName like %:query%
               or (c.TradeName is not null and c.TradeName like %:query%)
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select c.*
            from company c
            where c.CompanyCod = :id
               or c.TaxId = :query
               or c.LegalName like %:query%
               or (c.TradeName is not null and c.TradeName like %:query%)
            order by c.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<CompanyEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit);

    @Query(value = """
            select c.*
            from company c
            where c.Status = 'A'
            order by c.LegalName asc
            """, nativeQuery = true)
    List<CompanyEntity> findActives();

    @Query(value = """
            select c.* from company c where c.Status = 'A' limit 1
            """, nativeQuery = true)
    CompanyEntity findMyCompany();
}
