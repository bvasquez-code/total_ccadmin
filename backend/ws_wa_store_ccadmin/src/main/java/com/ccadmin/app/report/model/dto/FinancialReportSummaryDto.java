package com.ccadmin.app.report.model.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FinancialReportSummaryDto {
    public String CurrencyCod;
    public BigDecimal Sales = BigDecimal.ZERO;
    public BigDecimal Purchases = BigDecimal.ZERO;
    public BigDecimal Cost = BigDecimal.ZERO;
    public BigDecimal KnownResult = BigDecimal.ZERO;
    public BigDecimal Result;
    public BigDecimal Margin;
    public long MissingCostRows;
    public long RowCount;

    public FinancialReportSummaryDto(String currencyCod) {
        CurrencyCod = currencyCod;
    }

    public void add(FinancialReportDayDto day) {
        Sales = Sales.add(day.Sales);
        Purchases = Purchases.add(day.Purchases);
        Cost = Cost.add(day.Cost);
        KnownResult = KnownResult.add(day.KnownResult);
        MissingCostRows += day.MissingCostRows;
        RowCount += day.RowCount;
        Result = MissingCostRows == 0 ? KnownResult : null;
        Margin = Result != null && Sales.signum() > 0
                ? Result.multiply(BigDecimal.valueOf(100)).divide(Sales, 2, RoundingMode.HALF_UP)
                : null;
    }
}
