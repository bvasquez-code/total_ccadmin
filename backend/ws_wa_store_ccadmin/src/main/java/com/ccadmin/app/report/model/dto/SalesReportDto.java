package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.ISalesReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class SalesReportDto {
    public String SaleCod;
    public Date ReportDate;
    public String StoreName;
    public String ClientName;
    public String CurrencyCod;
    public String SaleStatus;
    public String IsPaid;
    public BigDecimal Subtotal;
    public BigDecimal Discount;
    public BigDecimal AmountNoTax;
    public BigDecimal Tax;
    public BigDecimal Amount;
    public String StoreCod;

    public SalesReportDto(ISalesReportDto row) {
        this.SaleCod = row.getSaleCod();
        this.ReportDate = row.getReportDate();
        this.StoreName = row.getStoreName();
        this.ClientName = row.getClientName();
        this.CurrencyCod = row.getCurrencyCod();
        this.SaleStatus = row.getSaleStatus();
        this.IsPaid = row.getIsPaid();
        this.Subtotal = row.getSubtotal();
        this.Discount = row.getDiscount();
        this.AmountNoTax = row.getAmountNoTax();
        this.Tax = row.getTax();
        this.Amount = row.getAmount();
        this.StoreCod = row.getStoreCod();
    }
}
