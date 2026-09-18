package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IPaymentMethodsReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class PaymentMethodsReportDto {
    public String StoreName;
    public String PaymentMethodName;
    public String CurrencyCod;
    public String PaymentStatus;
    public Long PaymentCount;
    public BigDecimal Income;
    public BigDecimal Reversals;
    public BigDecimal Amount;
    public String StoreCod;

    public PaymentMethodsReportDto(IPaymentMethodsReportDto row) {
        this.StoreName = row.getStoreName();
        this.PaymentMethodName = row.getPaymentMethodName();
        this.CurrencyCod = row.getCurrencyCod();
        this.PaymentStatus = row.getPaymentStatus();
        this.PaymentCount = row.getPaymentCount();
        this.Income = row.getIncome();
        this.Reversals = row.getReversals();
        this.Amount = row.getAmount();
        this.StoreCod = row.getStoreCod();
    }
}
