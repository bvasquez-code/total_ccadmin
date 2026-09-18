package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface IPaymentMethodsReportDto {
    String getStoreName();
    String getPaymentMethodName();
    String getCurrencyCod();
    String getPaymentStatus();
    Long getPaymentCount();
    BigDecimal getIncome();
    BigDecimal getReversals();
    BigDecimal getAmount();
    String getStoreCod();
}
