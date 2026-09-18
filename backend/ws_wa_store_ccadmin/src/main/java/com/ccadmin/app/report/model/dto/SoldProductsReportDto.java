package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.ISoldProductsReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class SoldProductsReportDto {
    public String ProductCod;
    public String ProductName;
    public String Variant;
    public String StoreName;
    public String ProductUnitName;
    public Long ProductUnitFactor;
    public String CurrencyCod;
    public Long SaleCount;
    public Long Quantity;
    public BigDecimal AmountNoTax;
    public BigDecimal Tax;
    public BigDecimal Amount;
    public String StoreCod;

    public SoldProductsReportDto(ISoldProductsReportDto row) {
        this.ProductCod = row.getProductCod();
        this.ProductName = row.getProductName();
        this.Variant = row.getVariant();
        this.StoreName = row.getStoreName();
        this.ProductUnitName = row.getProductUnitName();
        this.ProductUnitFactor = row.getProductUnitFactor();
        this.CurrencyCod = row.getCurrencyCod();
        this.SaleCount = row.getSaleCount();
        this.Quantity = row.getQuantity();
        this.AmountNoTax = row.getAmountNoTax();
        this.Tax = row.getTax();
        this.Amount = row.getAmount();
        this.StoreCod = row.getStoreCod();
    }
}
