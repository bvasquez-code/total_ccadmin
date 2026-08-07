package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.SaleAppliedTaxEntity;

import java.math.BigDecimal;

public final class SaleAppliedTaxEntityFactory {

    private SaleAppliedTaxEntityFactory() {
    }

    public static SaleAppliedTaxEntity fromTax(
            String taxCod,
            String saleCod,
            BigDecimal taxRateValue
    ) {
        SaleAppliedTaxEntity appliedTax = new SaleAppliedTaxEntity();
        appliedTax.TaxCod = taxCod;
        appliedTax.SaleCod = saleCod;
        appliedTax.TaxRateValue = taxRateValue;
        return appliedTax;
    }
}
