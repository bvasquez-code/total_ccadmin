package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.report.model.idto.IProfitReportDto;
import com.ccadmin.app.report.model.idto.IFinancialReportDayDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProfitReportRepository extends JpaRepository<SaleHeadEntity, String>,
        CcAdminRepository<IProfitReportDto, String> {

    // Detalle, conteo y resumen parten exactamente de las mismas operaciones y filtros.
    String REPORT_ROWS = """
            WITH operations AS (
                SELECT 'SALE' AS SourceType, 'sale_head' AS SourceTable,
                       s.SaleCod AS OperationCod, d.ItemNumber,
                       COALESCE((SELECT MIN(k.CreationDate) FROM kardex k
                                WHERE k.SourceTable = 'sale_head' AND k.OperationCod = s.SaleCod
                                  AND k.StoreCod = s.StoreCod AND k.TypeOperation = 'R'
                                  AND k.Status = 'A'), s.CreationDate) AS ReportDate,
                       s.StoreCod, s.CurrencyCod, d.ProductCod, d.Variant,
                       d.NumUnit AS Quantity, d.NumUnit AS ExpectedCostQuantity,
                       d.NumTotalPrice AS Amount, d.NumPriceSubTotal AS AmountNoTax, 1 AS CostSign
                FROM sale_head s JOIN sale_det d ON d.SaleCod = s.SaleCod AND d.Status = 'A'
                WHERE s.Status = 'A' AND s.SaleStatus = 'C' AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
                          AND EXISTS (SELECT 1 FROM user_store us WHERE us.StoreCod = s.StoreCod
                                      AND us.UserCod = :#{#search.UserCod} AND us.Status = 'A')
                          AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
                UNION ALL
                SELECT 'CREDIT_NOTE', 'credit_note_head', s.CreditNoteCod, d.ItemNumber,
                       COALESCE((SELECT MIN(k.CreationDate) FROM kardex k
                                WHERE k.SourceTable = 'credit_note_head' AND k.OperationCod = s.CreditNoteCod
                                  AND k.StoreCod = s.StoreCod AND k.TypeOperation = 'S'
                                  AND k.Status = 'A'), s.CreationDate),
                       s.StoreCod, s.CurrencyCod, d.ProductCod, d.Variant, -d.NumUnit,
                       CASE WHEN s.IsStockReturned = 'S' THEN COALESCE(d.NumUnitStockReturned, 0) ELSE NULL END,
                       -d.NumTotalPrice, -d.NumPriceSubTotal, -1
                FROM credit_note_head s JOIN credit_note_det d ON d.CreditNoteCod = s.CreditNoteCod AND d.Status = 'A'
                WHERE s.Status = 'A' AND s.CreditNoteStatus = 'C' AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
                          AND EXISTS (SELECT 1 FROM user_store us WHERE us.StoreCod = s.StoreCod
                                      AND us.UserCod = :#{#search.UserCod} AND us.Status = 'A')
                          AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
            ), movements AS (
                SELECT o.*, st.Name AS StoreName, pr.ProductName
                FROM operations o
                JOIN store st ON st.StoreCod = o.StoreCod
                LEFT JOIN product pr ON pr.ProductCod = o.ProductCod
                WHERE o.ReportDate >= :#{#search.DateFrom} AND o.ReportDate < :#{#search.DateTo}
                  AND CONCAT_WS(' ', o.OperationCod, o.ProductCod, pr.ProductName, o.Variant)
                      LIKE :#{#search.Query} ESCAPE '!'
            ), trace_cost AS (
                SELECT t.SourceTable, t.OperationCod, t.ItemNumber, t.ProductCod, t.Variant, t.StoreCod,
                       SUM(CASE WHEN t.SourceTable = 'credit_note_head' AND t.TypeOperation = 'R'
                                THEN -t.NumUnit ELSE t.NumUnit END) AS CostQuantity,
                       SUM(CASE WHEN t.SourceTable = 'credit_note_head' AND t.TypeOperation = 'R'
                                THEN -t.NumTotalPriceCost ELSE t.NumTotalPriceCost END) AS Cost,
                       MIN(purchase.CurrencyCod) AS MinCurrency, MAX(purchase.CurrencyCod) AS MaxCurrency,
                       SUM(CASE WHEN purchase.CurrencyCod IS NULL OR purchase.CurrencyCod = ''
                                THEN 1 ELSE 0 END) AS UnverifiedAllocations
                FROM product_traceability t
                JOIN movements m ON m.SourceTable = t.SourceTable AND m.OperationCod = t.OperationCod
                     AND m.ItemNumber = t.ItemNumber AND m.StoreCod = t.StoreCod
                     AND m.ProductCod = t.ProductCod AND m.Variant = t.Variant
                LEFT JOIN product_traceability origin ON origin.ProductTraceabilityID = (
                    SELECT MIN(root.ProductTraceabilityID) FROM product_traceability root
                    WHERE root.TechnicalLot = t.TechnicalLot AND root.OriginProductTraceabilityID IS NULL
                      AND root.Status = 'A'
                )
                LEFT JOIN pucharse_head purchase ON origin.SourceTable = 'pucharse_head'
                     AND purchase.PucharseCod = origin.OperationCod AND purchase.Status = 'A'
                     AND purchase.PurchaseStatus = 'F'
                WHERE t.Status = 'A' AND (
                    (t.SourceTable = 'sale_head' AND t.TypeOperation = 'R')
                    OR (t.SourceTable = 'credit_note_head' AND t.TypeOperation IN ('S', 'R'))
                )
                GROUP BY t.SourceTable, t.OperationCod, t.ItemNumber, t.ProductCod, t.Variant, t.StoreCod
            ), priced AS (
                SELECT m.*,
                       CASE WHEN m.ExpectedCostQuantity = 0 THEN 0
                            WHEN tc.CostQuantity = m.ExpectedCostQuantity AND tc.UnverifiedAllocations = 0
                                 AND tc.MinCurrency = m.CurrencyCod AND tc.MaxCurrency = m.CurrencyCod
                            THEN tc.Cost * m.CostSign ELSE NULL END AS Cost
                FROM movements m LEFT JOIN trace_cost tc
                     ON tc.SourceTable = m.SourceTable AND tc.OperationCod = m.OperationCod
                     AND tc.ItemNumber = m.ItemNumber AND tc.StoreCod = m.StoreCod
                     AND tc.ProductCod = m.ProductCod AND tc.Variant = m.Variant
            ), report_rows AS (
                SELECT r.*, r.Amount - r.Cost AS Profit,
                       CASE WHEN r.Amount > 0 THEN ROUND(100 * (r.Amount - r.Cost) / r.Amount, 2)
                            ELSE NULL END AS Margin,
                       CASE WHEN r.Cost IS NULL THEN 'MISSING' ELSE 'COMPLETE' END AS CostStatus
                FROM priced r
                WHERE (:#{#search.State} = '' OR (:#{#search.State} = 'MISSING' AND r.Cost IS NULL)
                       OR (:#{#search.State} = 'COMPLETE' AND r.Cost IS NOT NULL))
            )
            """;

    @Override
    @Query(value = REPORT_ROWS + "SELECT COUNT(*) FROM report_rows", nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = REPORT_ROWS + """
            SELECT * FROM report_rows
            ORDER BY ReportDate, SourceType, OperationCod, ItemNumber
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<IProfitReportDto> findByQueryText(@Param("search") SearchDto search);

    @Query(value = REPORT_ROWS + """
            SELECT DATE_FORMAT(ReportDate, '%Y-%m-%d') AS ReportDay, CurrencyCod,
                   SUM(Amount) AS Sales, 0 AS Purchases, COALESCE(SUM(Cost), 0) AS Cost,
                   COALESCE(SUM(Profit), 0) AS KnownResult,
                   SUM(CASE WHEN Cost IS NULL THEN 1 ELSE 0 END) AS MissingCostRows, COUNT(*) AS RowCount
            FROM report_rows
            GROUP BY ReportDay, CurrencyCod
            ORDER BY ReportDay, CurrencyCod
            """, nativeQuery = true)
    List<IFinancialReportDayDto> findDaily(@Param("search") SearchDto search);
}
