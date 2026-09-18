package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface IStockReportDto {
    String getProductCod();
    String getProductName();
    String getVariant();
    String getStoreName();
    String getProductUnitName();
    Long getProductUnitFactor();
    Long getPhysicalStock();
    Long getReservedStock();
    Long getUnavailableStock();
    Long getTotalStock();
    Long getMinStock();
    String getStockStatus();
    String getStoreCod();
}
