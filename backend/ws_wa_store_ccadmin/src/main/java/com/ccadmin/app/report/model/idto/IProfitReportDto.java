package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface IProfitReportDto {
    String getSourceType();
    String getOperationCod();
    Long getItemNumber();
    Date getReportDate();
    String getStoreName();
    String getProductCod();
    String getProductName();
    String getVariant();
    String getCurrencyCod();
    Long getQuantity();
    BigDecimal getAmount();
    BigDecimal getAmountNoTax();
    BigDecimal getCost();
    BigDecimal getProfit();
    BigDecimal getMargin();
    String getCostStatus();
    String getStoreCod();
}
