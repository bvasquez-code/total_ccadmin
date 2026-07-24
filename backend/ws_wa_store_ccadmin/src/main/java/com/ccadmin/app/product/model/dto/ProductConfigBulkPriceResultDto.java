package com.ccadmin.app.product.model.dto;

import java.math.BigDecimal;

public class ProductConfigBulkPriceResultDto {
    public Integer ReferenceItemNumber;
    public BigDecimal OldPrice;
    public BigDecimal NewPrice;
    public Boolean Changed;
}
