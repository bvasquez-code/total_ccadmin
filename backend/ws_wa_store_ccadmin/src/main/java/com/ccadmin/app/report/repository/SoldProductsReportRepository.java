package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.report.model.idto.ISoldProductsReportDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SoldProductsReportRepository extends JpaRepository<SaleHeadEntity, String>, CcAdminRepository<ISoldProductsReportDto, String> {

    @Override
    @Query(value = """
            SELECT COUNT(*) FROM (
            SELECT d.ProductCod AS ProductCod, pr.ProductName AS ProductName, d.Variant AS Variant,
                   d.ProductUnitName AS ProductUnitName, d.ProductUnitFactor AS ProductUnitFactor,
                   s.StoreCod AS StoreCod, st.Name AS StoreName, s.CurrencyCod AS CurrencyCod,
                   COUNT(DISTINCT s.SaleCod) AS SaleCount, SUM(d.NumUnit) AS Quantity,
                   SUM(d.NumPriceSubTotal) AS AmountNoTax,
                   SUM(d.NumTotalTax) AS Tax, SUM(d.NumTotalPrice) AS Amount
            FROM sale_head s
            JOIN sale_det d ON d.SaleCod = s.SaleCod AND d.Status = 'A'
            LEFT JOIN product pr ON pr.ProductCod = d.ProductCod
            JOIN store st ON st.StoreCod = s.StoreCod
            WHERE s.Status = 'A' AND s.SaleStatus = 'C' AND :#{#search.State} = ''
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', d.ProductCod, pr.ProductName, d.Variant) LIKE :#{#search.Query} ESCAPE '!' GROUP BY d.ProductCod, pr.ProductName, d.Variant, d.ProductUnitName,
                     d.ProductUnitFactor, s.StoreCod, st.Name, s.CurrencyCod

            ) report_count
            """, nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = """
            SELECT d.ProductCod AS ProductCod, pr.ProductName AS ProductName, d.Variant AS Variant,
                   d.ProductUnitName AS ProductUnitName, d.ProductUnitFactor AS ProductUnitFactor,
                   s.StoreCod AS StoreCod, st.Name AS StoreName, s.CurrencyCod AS CurrencyCod,
                   COUNT(DISTINCT s.SaleCod) AS SaleCount, SUM(d.NumUnit) AS Quantity,
                   SUM(d.NumPriceSubTotal) AS AmountNoTax,
                   SUM(d.NumTotalTax) AS Tax, SUM(d.NumTotalPrice) AS Amount
            FROM sale_head s
            JOIN sale_det d ON d.SaleCod = s.SaleCod AND d.Status = 'A'
            LEFT JOIN product pr ON pr.ProductCod = d.ProductCod
            JOIN store st ON st.StoreCod = s.StoreCod
            WHERE s.Status = 'A' AND s.SaleStatus = 'C' AND :#{#search.State} = ''
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', d.ProductCod, pr.ProductName, d.Variant) LIKE :#{#search.Query} ESCAPE '!' GROUP BY d.ProductCod, pr.ProductName, d.Variant, d.ProductUnitName,
                     d.ProductUnitFactor, s.StoreCod, st.Name, s.CurrencyCod

            ORDER BY Amount DESC, StoreCod, CurrencyCod, ProductCod, Variant, ProductUnitName, ProductUnitFactor
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<ISoldProductsReportDto> findByQueryText(@Param("search") SearchDto search);
}
