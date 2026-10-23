package com.ccadmin.app.report.repository;

/** Origen y moneda verificable del costo de una asignación de product_traceability (alias trace). */
final class ReportTraceCostSql {
    private ReportTraceCostSql() {
    }

    static final String ORIGIN_JOINS = """
            LEFT JOIN product_traceability costOrigin ON costOrigin.ProductTraceabilityID = (
                SELECT MIN(root.ProductTraceabilityID) FROM product_traceability root
                WHERE root.TechnicalLot = trace.TechnicalLot AND root.OriginProductTraceabilityID IS NULL
                  AND root.Status = 'A'
            )
            LEFT JOIN pucharse_head costPurchase ON costOrigin.SourceTable = 'pucharse_head'
                 AND costPurchase.PucharseCod = costOrigin.OperationCod
            LEFT JOIN stock_entry_head costStockEntry ON costOrigin.SourceTable = 'stock_entry_head'
                 AND costStockEntry.StockEntryCod = costOrigin.OperationCod
                 AND costStockEntry.Status = 'A' AND costStockEntry.ProcessStatus = 'C'
            LEFT JOIN stock_entry_det costStockEntryDetail
                 ON costStockEntryDetail.StockEntryCod = costStockEntry.StockEntryCod
                 AND costStockEntryDetail.ItemNumber = costOrigin.ItemNumber
                 AND costStockEntryDetail.ProductCod = costOrigin.ProductCod
                 AND costStockEntryDetail.Variant = costOrigin.Variant
                 AND costStockEntryDetail.Status = 'A'
            """;

    // Las entradas no tienen moneda propia: sus precios se expresan en la moneda del sistema.
    // Un precio omitido se guarda como cero; no debe verificarse como mercadería sin costo.
    static final String VERIFIED_CURRENCY = """
            CASE WHEN trace.NumTotalPriceCost IS NULL THEN NULL
                 WHEN costOrigin.SourceTable = 'pucharse_head'
                      AND costPurchase.Status = 'A' AND costPurchase.PurchaseStatus = 'F'
                 THEN NULLIF(costPurchase.CurrencyCod, '')
                 WHEN costOrigin.SourceTable = 'stock_entry_head'
                      AND costStockEntryDetail.NumUnitPrice > 0
                      AND costOrigin.NumTotalPriceCost > 0 AND trace.NumTotalPriceCost > 0
                 THEN (SELECT MAX(currency.CurrencyCod) FROM currency currency
                       WHERE currency.IsMonedaSystem = 'S' HAVING COUNT(*) = 1)
                 ELSE NULL END
            """;
}
