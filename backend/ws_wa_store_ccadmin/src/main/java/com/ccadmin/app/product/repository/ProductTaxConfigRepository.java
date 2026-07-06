package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductTaxConfigRepository extends JpaRepository<ProductTaxConfigEntity, Long> {

    @Query(value = """
            select ptc.*
            from product_tax_config ptc
            where ptc.ProductCod = :ProductCod
              and ptc.StoreCod = :StoreCod
            order by ptc.Status, ptc.IsMainTax desc, ptc.CalculationOrder, ptc.TaxCod
            """, nativeQuery = true)
    List<ProductTaxConfigEntity> findByProductAndStore(
            @Param("ProductCod") String ProductCod,
            @Param("StoreCod") String StoreCod
    );

    @Query(value = """
            select ptc.*
            from product_tax_config ptc
            where ptc.ProductCod = :ProductCod
              and ptc.StoreCod = :StoreCod
              and ptc.Status = 'A'
            order by ptc.IsMainTax desc, ptc.CalculationOrder, ptc.TaxCod
            """, nativeQuery = true)
    List<ProductTaxConfigEntity> findActiveByProductAndStore(
            @Param("ProductCod") String ProductCod,
            @Param("StoreCod") String StoreCod
    );

    @Query(value = """
            select count(1)
            from product_tax_config ptc
            where ptc.ProductCod = :ProductCod
              and ptc.StoreCod = :StoreCod
              and ptc.TaxCod = :TaxCod
              and ptc.Status = 'A'
              and (:ProductTaxConfigId is null or ptc.ProductTaxConfigId <> :ProductTaxConfigId)
            """, nativeQuery = true)
    int countActiveTax(
            @Param("ProductCod") String ProductCod,
            @Param("StoreCod") String StoreCod,
            @Param("TaxCod") String TaxCod,
            @Param("ProductTaxConfigId") Long ProductTaxConfigId
    );

    @Query(value = """
            select count(1)
            from product_tax_config ptc
            where ptc.ProductCod = :ProductCod
              and ptc.StoreCod = :StoreCod
              and ptc.IsMainTax = 'S'
              and ptc.Status = 'A'
              and (:ProductTaxConfigId is null or ptc.ProductTaxConfigId <> :ProductTaxConfigId)
            """, nativeQuery = true)
    int countActiveMainTax(
            @Param("ProductCod") String ProductCod,
            @Param("StoreCod") String StoreCod,
            @Param("ProductTaxConfigId") Long ProductTaxConfigId
    );
}
