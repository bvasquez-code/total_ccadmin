package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;

public interface IFinancialReportDayDto {
    String getReportDay();
    String getCurrencyCod();
    BigDecimal getSales();
    BigDecimal getPurchases();
    BigDecimal getCost();
    BigDecimal getKnownResult();
    long getMissingCostRows();
    long getRowCount();
}
