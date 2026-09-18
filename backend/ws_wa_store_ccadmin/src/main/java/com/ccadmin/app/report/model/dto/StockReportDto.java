package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IStockReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class StockReportDto {
    public String ProductCod;
    public String ProductName;
    public String Variant;
    public String StoreName;
    public String ProductUnitName;
    public Long ProductUnitFactor;
    public Long PhysicalStock;
    public Long ReservedStock;
    public Long UnavailableStock;
    public Long TotalStock;
    public Long MinStock;
    public String StockStatus;
    public String StoreCod;

    public StockReportDto(IStockReportDto row) {
        this.ProductCod = row.getProductCod();
        this.ProductName = row.getProductName();
        this.Variant = row.getVariant();
        this.StoreName = row.getStoreName();
        this.ProductUnitName = row.getProductUnitName();
        this.ProductUnitFactor = row.getProductUnitFactor();
        this.PhysicalStock = row.getPhysicalStock();
        this.ReservedStock = row.getReservedStock();
        this.UnavailableStock = row.getUnavailableStock();
        this.TotalStock = row.getTotalStock();
        this.MinStock = row.getMinStock();
        this.StockStatus = row.getStockStatus();
        this.StoreCod = row.getStoreCod();
    }
}
