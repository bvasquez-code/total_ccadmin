package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface IPurchaseSalesReportDto {
    String getSourceType();
    String getSourceTable();
    String getTypeOperation();
    BigDecimal getQuantity();
    String getOperationCod();
    Date getReportDate();
    String getStoreCod();
    String getStoreName();
    String getCurrencyCod();
    String getPartnerCod();
    String getReference();
    BigDecimal getSales();
    BigDecimal getPurchases();
    BigDecimal getBalance();
}
