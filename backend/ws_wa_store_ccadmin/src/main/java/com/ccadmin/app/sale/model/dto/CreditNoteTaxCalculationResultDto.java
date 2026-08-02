package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.CreditNoteDetTaxEntity;

import java.math.BigDecimal;
import java.util.List;

public class CreditNoteTaxCalculationResultDto {

    public List<CreditNoteDetTaxEntity> TaxDetailList = List.of();
    public BigDecimal NumTotalTax = BigDecimal.ZERO;
    public BigDecimal NumPriceSubTotal = BigDecimal.ZERO;
    public String IsAppliedTax = "N";
}
