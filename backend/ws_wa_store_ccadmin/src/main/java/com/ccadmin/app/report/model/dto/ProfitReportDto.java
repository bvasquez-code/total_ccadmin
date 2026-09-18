package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IProfitReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class ProfitReportDto {
    public String SourceType;
    public String OperationCod;
    public Long ItemNumber;
    public Date ReportDate;
    public String StoreName;
    public String ProductCod;
    public String ProductName;
    public String Variant;
    public String CurrencyCod;
    public Long Quantity;
    public BigDecimal Amount;
    public BigDecimal AmountNoTax;
    public BigDecimal Cost;
    public BigDecimal Profit;
    public BigDecimal Margin;
    public String CostStatus;
    public String StoreCod;

    public ProfitReportDto(IProfitReportDto row) {
        this.SourceType = row.getSourceType();
        this.OperationCod = row.getOperationCod();
        this.ItemNumber = row.getItemNumber();
        this.ReportDate = row.getReportDate();
        this.StoreName = row.getStoreName();
        this.ProductCod = row.getProductCod();
        this.ProductName = row.getProductName();
        this.Variant = row.getVariant();
        this.CurrencyCod = row.getCurrencyCod();
        this.Quantity = row.getQuantity();
        this.Amount = row.getAmount();
        this.AmountNoTax = row.getAmountNoTax();
        this.Cost = row.getCost();
        this.Profit = row.getProfit();
        this.Margin = row.getMargin();
        this.CostStatus = row.getCostStatus();
        this.StoreCod = row.getStoreCod();
    }
}
