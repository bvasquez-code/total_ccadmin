package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;

public interface IMerchandiseProfitabilityDto {
    String getCurrencyCod();
    BigDecimal getNetSales();
    BigDecimal getCostOfGoodsSold();
    BigDecimal getStockLosses();
    BigDecimal getSoldQuantity();
    BigDecimal getLostQuantity();
    long getMissingCostRows();
}
