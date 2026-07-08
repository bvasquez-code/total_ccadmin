package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.id.SaleDetTaxID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleDetTaxRepository extends JpaRepository<SaleDetTaxEntity, SaleDetTaxID> {

    @Query(value = """
            select sdt.*
            from sale_det_tax sdt
            where sdt.SaleCod = :SaleCod
              and sdt.Status = 'A'
            order by sdt.ItemNumber, sdt.TaxLineNumber
            """, nativeQuery = true)
    List<SaleDetTaxEntity> findBySaleCod(@Param("SaleCod") String SaleCod);

    @Query(value = """
            select sdt.*
            from sale_det_tax sdt
            where sdt.SaleCod = :SaleCod
              and sdt.ItemNumber = :ItemNumber
              and sdt.Status = 'A'
            order by sdt.TaxLineNumber
            """, nativeQuery = true)
    List<SaleDetTaxEntity> findBySaleCodAndItemNumber(
            @Param("SaleCod") String SaleCod,
            @Param("ItemNumber") int ItemNumber
    );
}
