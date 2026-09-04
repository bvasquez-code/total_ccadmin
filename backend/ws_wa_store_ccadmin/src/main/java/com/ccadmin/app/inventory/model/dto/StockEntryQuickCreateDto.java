package com.ccadmin.app.inventory.model.dto;

import java.math.BigDecimal;
import java.util.Date;

public class StockEntryQuickCreateDto {
    public String ProductCod;
    public Integer Quantity;
    public BigDecimal NumUnitPrice = BigDecimal.ZERO;
    public String LotNumber;
    public Date ExpirationDate;
}
