package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.TaxAffectationEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaxAffectationRepository extends JpaRepository<TaxAffectationEntity, String>,
        CcAdminRepository<TaxAffectationEntity, String> {

    @Query(value = """
            select ta.*
            from tax_affectation ta
            where ta.Status = 'A'
            order by ta.TaxAffectationCod
            """, nativeQuery = true)
    List<TaxAffectationEntity> findAllActive();

    @Query(value = """
            select ta.*
            from tax_affectation ta
            where ta.TaxAffectationCod = :TaxAffectationCod
              and ta.TaxCod = :TaxCod
              and ta.Status = 'A'
            """, nativeQuery = true)
    TaxAffectationEntity findActiveByCodeAndTax(
            @Param("TaxAffectationCod") String TaxAffectationCod,
            @Param("TaxCod") String TaxCod
    );

    @Override
    @Query(value = """
            select count(1)
            from tax_affectation ta
            join tax t on t.TaxCod = ta.TaxCod
            where ta.TaxAffectationCod = :id
               or ta.Name like %:query%
               or ta.Description like %:query%
               or ta.TaxCod = :id
               or t.Name like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select ta.*
            from tax_affectation ta
            join tax t on t.TaxCod = ta.TaxCod
            where ta.TaxAffectationCod = :id
               or ta.Name like %:query%
               or ta.Description like %:query%
               or ta.TaxCod = :id
               or t.Name like %:query%
            order by ta.TaxAffectationCod
            limit :init, :limit
            """, nativeQuery = true)
    List<TaxAffectationEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
