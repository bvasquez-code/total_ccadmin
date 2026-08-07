package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.CreditNoteTaxCalculationResultDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDetTaxEntity;

import java.math.BigDecimal;
import java.util.List;

public final class CreditNoteTaxCalculationResultDtoFactory {

    private CreditNoteTaxCalculationResultDtoFactory() {
    }

    public static CreditNoteTaxCalculationResultDto fromCalculation(
            List<CreditNoteDetTaxEntity> taxDetails,
            BigDecimal totalTax,
            BigDecimal priceSubtotal,
            String isAppliedTax
    ) {
        CreditNoteTaxCalculationResultDto result = new CreditNoteTaxCalculationResultDto();
        result.TaxDetailList = taxDetails;
        result.NumTotalTax = totalTax;
        result.NumPriceSubTotal = priceSubtotal;
        result.IsAppliedTax = isAppliedTax;
        return result;
    }
}
