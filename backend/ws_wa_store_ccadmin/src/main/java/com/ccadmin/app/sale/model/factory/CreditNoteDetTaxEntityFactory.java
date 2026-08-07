package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetTaxEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;

import java.math.BigDecimal;

public final class CreditNoteDetTaxEntityFactory {

    private CreditNoteDetTaxEntityFactory() {
    }

    public static CreditNoteDetTaxEntity fromSaleTax(
            String creditNoteCod,
            CreditNoteDetEntity creditNoteDetail,
            SaleDetTaxEntity saleTax,
            BigDecimal taxBaseAmount,
            BigDecimal taxQuantity,
            BigDecimal taxAmount
    ) {
        CreditNoteDetTaxEntity tax = new CreditNoteDetTaxEntity();
        tax.CreditNoteCod = creditNoteCod;
        tax.ItemNumber = creditNoteDetail.ItemNumber;
        tax.TaxLineNumber = saleTax.TaxLineNumber;
        tax.TaxCod = saleTax.TaxCod;
        tax.SunatTaxCod = saleTax.SunatTaxCod;
        tax.TaxName = saleTax.TaxName;
        tax.TaxAffectationCod = saleTax.TaxAffectationCod;
        tax.TaxAffectationName = saleTax.TaxAffectationName;
        tax.TaxCalculationType = saleTax.TaxCalculationType;
        tax.IsInformative = saleTax.IsInformative;
        tax.TaxRateValue = saleTax.TaxRateValue;
        tax.FixedUnitAmount = saleTax.FixedUnitAmount;
        tax.TaxBaseAmount = taxBaseAmount;
        tax.TaxQuantity = taxQuantity;
        tax.TaxAmount = taxAmount;
        tax.CalculationOrder = saleTax.CalculationOrder;
        return tax;
    }
}
