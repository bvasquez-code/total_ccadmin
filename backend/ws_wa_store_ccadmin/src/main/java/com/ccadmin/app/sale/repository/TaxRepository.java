package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.TaxEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaxRepository extends JpaRepository<TaxEntity,String>, CcAdminRepository<TaxEntity, String> {

    @Query( value = """
            select * from tax t where t.Status = 'A'
            order by t.CalculationOrder, t.TaxCod
            """,nativeQuery = true)
    public List<TaxEntity> findAllActive();

    @Override
    @Query(value = """
            select count(1)
            from tax t
            where t.TaxCod = :id
               or t.SunatTaxCod = :id
               or t.Name like %:query%
               or t.Description like %:query%
               or t.TaxCalculationType like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select t.*
            from tax t
            where t.TaxCod = :id
               or t.SunatTaxCod = :id
               or t.Name like %:query%
               or t.Description like %:query%
               or t.TaxCalculationType like %:query%
            order by t.CalculationOrder, t.TaxCod
            limit :init, :limit
            """, nativeQuery = true)
    List<TaxEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
