package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SaleTaxCalculationResultDto {

    public List<SaleDetEntity> DetailList = new ArrayList<>();
    public List<SaleDetTaxEntity> TaxDetailList = new ArrayList<>();
    public BigDecimal NumTotalPriceNoTax = BigDecimal.ZERO;
    public BigDecimal NumTotalTax = BigDecimal.ZERO;
    public BigDecimal NumTotalPrice = BigDecimal.ZERO;

}
