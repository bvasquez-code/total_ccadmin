package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface ISalesReportDto {
    String getSaleCod();
    Date getReportDate();
    String getStoreName();
    String getClientName();
    String getCurrencyCod();
    String getSaleStatus();
    String getIsPaid();
    BigDecimal getSubtotal();
    BigDecimal getDiscount();
    BigDecimal getAmountNoTax();
    BigDecimal getTax();
    BigDecimal getAmount();
    String getStoreCod();
}
