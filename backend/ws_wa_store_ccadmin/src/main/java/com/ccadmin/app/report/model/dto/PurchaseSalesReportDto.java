package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IPurchaseSalesReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class PurchaseSalesReportDto {
    public String SourceType;
    public String SourceTable;
    public String TypeOperation;
    public BigDecimal Quantity;
    public String OperationCod;
    public Date ReportDate;
    public String StoreCod;
    public String StoreName;
    public String CurrencyCod;
    public String PartnerCod;
    public String Reference;
    public BigDecimal Sales;
    public BigDecimal Purchases;
    public BigDecimal Balance;

    public PurchaseSalesReportDto(IPurchaseSalesReportDto row) {
        this.SourceType = row.getSourceType();
        this.SourceTable = row.getSourceTable();
        this.TypeOperation = row.getTypeOperation();
        this.Quantity = row.getQuantity();
        this.OperationCod = row.getOperationCod();
        this.ReportDate = row.getReportDate();
        this.StoreCod = row.getStoreCod();
        this.StoreName = row.getStoreName();
        this.CurrencyCod = row.getCurrencyCod();
        this.PartnerCod = row.getPartnerCod();
        this.Reference = row.getReference();
        this.Sales = row.getSales();
        this.Purchases = row.getPurchases();
        this.Balance = row.getBalance();
    }
}
