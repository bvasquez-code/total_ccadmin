package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface ISoldProductsReportDto {
    String getProductCod();
    String getProductName();
    String getVariant();
    String getStoreName();
    String getProductUnitName();
    Long getProductUnitFactor();
    String getCurrencyCod();
    Long getSaleCount();
    Long getQuantity();
    BigDecimal getAmountNoTax();
    BigDecimal getTax();
    BigDecimal getAmount();
    String getStoreCod();
}
