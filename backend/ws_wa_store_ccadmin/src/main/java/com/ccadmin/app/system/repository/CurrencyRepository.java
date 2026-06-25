package com.ccadmin.app.system.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CurrencyRepository extends JpaRepository<CurrencyEntity,String>, CcAdminRepository<CurrencyEntity, String> {

    @Query(value = """
            select * from currency c where c.IsCurrencySystem = 'S' order by c.CreationDate desc  limit 1
            """,nativeQuery = true)
    public CurrencyEntity findCurrencySystem();
    @Query(value = """
            select c.* from currency c where c.Status = 'A'
            """,nativeQuery = true)
    public List<CurrencyEntity> findAllActive();

    @Override
    @Query(value = """
            select count(1)
            from currency c
            where c.CurrencyCod = :id
               or c.CurrencyAbbr like %:query%
               or c.CurrencySymbol like %:query%
               or c.CurrencyName like %:query%
               or c.CurrencyDesc like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select c.*
            from currency c
            where c.CurrencyCod = :id
               or c.CurrencyAbbr like %:query%
               or c.CurrencySymbol like %:query%
               or c.CurrencyName like %:query%
               or c.CurrencyDesc like %:query%
            order by c.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<CurrencyEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
