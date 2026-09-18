package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.report.model.idto.IPurchaseSalesReportDto;
import com.ccadmin.app.report.model.idto.IFinancialReportDayDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PurchaseSalesReportRepository extends JpaRepository<SaleHeadEntity, String>,
        CcAdminRepository<IPurchaseSalesReportDto, String> {

    // La trazabilidad determina tienda, fecha, dirección e importe de cada movimiento.
    // Los documentos solo aportan referencias y moneda; sus totales no se vuelven a sumar.
    String REPORT_ROWS = """
            WITH movements AS (
                SELECT trace.OperationCod, trace.SourceTable, trace.TypeOperation,
                       trace.OperationDate AS ReportDate, trace.StoreCod, trace.NumUnit,
                       CASE trace.SourceTable
                           WHEN 'pucharse_head' THEN 'PURCHASE'
                           WHEN 'sale_head' THEN 'SALE'
                           WHEN 'credit_note_head' THEN 'CREDIT_NOTE'
                           WHEN 'stock_entry_head' THEN 'STOCK_ENTRY'
                           WHEN 'stock_exit_head' THEN 'STOCK_EXIT'
                           WHEN 'transfer_head' THEN 'TRANSFER'
                       END AS SourceType,
                       COALESCE(sale.CurrencyCod, creditNote.CurrencyCod, purchase.CurrencyCod,
                                lotPurchase.CurrencyCod,
                                (SELECT currency.CurrencyCod FROM currency currency
                                 WHERE currency.IsMonedaSystem = 'S' ORDER BY currency.CurrencyCod LIMIT 1),
                                'UNKNOWN') AS CurrencyCod,
                       COALESCE(sale.ClientCod, creditNote.ClientCod, purchase.DealerCod) AS PartnerCod,
                       COALESCE(sale.PresaleCod, creditNote.SaleCod, purchase.ExternalCod,
                                stockEntry.ReasonCode, stockExit.ReasonCode,
                                CONCAT(transfer.StoreCodOrigin, ' -> ', transfer.StoreCodDest)) AS Reference,
                       CASE WHEN trace.SourceTable = 'sale_head'
                                  OR (trace.SourceTable = 'credit_note_head' AND trace.TypeOperation = 'S')
                            THEN trace.NumTotalPriceSale ELSE trace.NumTotalPriceCost END AS Amount
                FROM product_traceability trace
                LEFT JOIN sale_head sale ON trace.SourceTable = 'sale_head' AND sale.SaleCod = trace.OperationCod
                LEFT JOIN credit_note_head creditNote ON trace.SourceTable = 'credit_note_head'
                                                     AND creditNote.CreditNoteCod = trace.OperationCod
                LEFT JOIN pucharse_head purchase ON trace.SourceTable = 'pucharse_head'
                                                AND purchase.PucharseCod = trace.OperationCod
                LEFT JOIN stock_entry_head stockEntry ON trace.SourceTable = 'stock_entry_head'
                                                     AND stockEntry.StockEntryCod = trace.OperationCod
                LEFT JOIN stock_exit_head stockExit ON trace.SourceTable = 'stock_exit_head'
                                                   AND stockExit.StockExitCod = trace.OperationCod
                LEFT JOIN transfer_head transfer ON trace.SourceTable = 'transfer_head'
                                                 AND transfer.TransferCod = trace.OperationCod
                LEFT JOIN product_traceability lotOrigin ON lotOrigin.ProductTraceabilityID = (
                    SELECT MIN(root.ProductTraceabilityID) FROM product_traceability root
                    WHERE root.TechnicalLot = trace.TechnicalLot AND root.OriginProductTraceabilityID IS NULL
                      AND root.Status = 'A'
                )
                LEFT JOIN pucharse_head lotPurchase ON lotOrigin.SourceTable = 'pucharse_head'
                                                   AND lotPurchase.PucharseCod = lotOrigin.OperationCod
                WHERE trace.Status = 'A'
                  AND (:#{#search.StoreCod} = '' OR trace.StoreCod = :#{#search.StoreCod})
                  AND trace.TypeOperation IN ('S', 'R')
                  AND trace.SourceTable IN ('pucharse_head', 'sale_head', 'credit_note_head',
                                            'stock_entry_head', 'stock_exit_head', 'transfer_head')
                  AND trace.OperationDate >= :#{#search.DateFrom} AND trace.OperationDate < :#{#search.DateTo}
                  AND EXISTS (SELECT 1 FROM user_store us WHERE us.StoreCod = trace.StoreCod
                              AND us.UserCod = :#{#search.UserCod} AND us.Status = 'A')
            ), operations AS (
                SELECT SourceType, SourceTable, OperationCod, TypeOperation, MIN(ReportDate) AS ReportDate,
                       StoreCod, CurrencyCod, PartnerCod, Reference, SUM(NumUnit) AS Quantity,
                       SUM(CASE WHEN TypeOperation = 'R' THEN Amount ELSE 0 END) AS Sales,
                       SUM(CASE WHEN TypeOperation = 'S' THEN Amount ELSE 0 END) AS Purchases
                FROM movements
                WHERE (:#{#search.CurrencyCod} = '' OR CurrencyCod = :#{#search.CurrencyCod})
                  AND (:#{#search.State} = '' OR SourceType = :#{#search.State})
                  AND CONCAT_WS(' ', OperationCod, PartnerCod, Reference) LIKE :#{#search.Query} ESCAPE '!'
                GROUP BY SourceType, SourceTable, OperationCod, TypeOperation, DATE(ReportDate),
                         StoreCod, CurrencyCod, PartnerCod, Reference
            ), report_rows AS (
                SELECT operations.*, store.Name AS StoreName, Sales - Purchases AS Balance
                FROM operations JOIN store store ON store.StoreCod = operations.StoreCod
            )
            """;

    @Override
    @Query(value = REPORT_ROWS + "SELECT COUNT(*) FROM report_rows", nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = REPORT_ROWS + """
            SELECT * FROM report_rows
            ORDER BY ReportDate, SourceType, OperationCod, StoreCod, TypeOperation, CurrencyCod
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<IPurchaseSalesReportDto> findByQueryText(@Param("search") SearchDto search);

    @Query(value = REPORT_ROWS + """
            SELECT DATE_FORMAT(ReportDate, '%Y-%m-%d') AS ReportDay, CurrencyCod,
                   SUM(Sales) AS Sales, SUM(Purchases) AS Purchases, 0 AS Cost,
                   SUM(Balance) AS KnownResult, 0 AS MissingCostRows, COUNT(*) AS RowCount
            FROM report_rows
            GROUP BY ReportDay, CurrencyCod
            ORDER BY ReportDay, CurrencyCod
            """, nativeQuery = true)
    List<IFinancialReportDayDto> findDaily(@Param("search") SearchDto search);
}
