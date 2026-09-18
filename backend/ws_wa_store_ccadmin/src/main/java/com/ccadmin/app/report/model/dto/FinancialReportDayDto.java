package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IFinancialReportDayDto;
import java.math.BigDecimal;

public class FinancialReportDayDto {
    public String ReportDay;
    public String CurrencyCod;
    public BigDecimal Sales;
    public BigDecimal Purchases;
    public BigDecimal Cost;
    public BigDecimal KnownResult;
    public long MissingCostRows;
    public long RowCount;

    public FinancialReportDayDto(IFinancialReportDayDto row) {
        this.ReportDay = row.getReportDay();
        this.CurrencyCod = row.getCurrencyCod();
        this.Sales = row.getSales();
        this.Purchases = row.getPurchases();
        this.Cost = row.getCost();
        this.KnownResult = row.getKnownResult();
        this.MissingCostRows = row.getMissingCostRows();
        this.RowCount = row.getRowCount();
    }
}
