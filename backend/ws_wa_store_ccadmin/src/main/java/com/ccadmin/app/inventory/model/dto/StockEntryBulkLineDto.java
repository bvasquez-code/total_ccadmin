package com.ccadmin.app.inventory.model.dto;

import java.math.BigDecimal;
import java.util.Date;

public class StockEntryBulkLineDto {
    public Integer ReferenceItemNumber;
    public Integer SourceRowNumber;
    public String ProductCod;
    public String Variant;
    public String WarehouseCod;
    public String ProductUnitName;
    public Integer ProductUnitFactor;
    public Integer NumUnit;
    public BigDecimal NumUnitPrice = BigDecimal.ZERO;
    public String LotNumber;
    public Date ExpirationDate;
}
