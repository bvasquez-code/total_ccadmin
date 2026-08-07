package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.SaleTaxCalculationResultDto;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class SaleTaxCalculationResultDtoFactory {

    private SaleTaxCalculationResultDtoFactory() {
    }

    public static SaleTaxCalculationResultDto fromLines(
            List<SaleDetEntity> details,
            List<SaleDetTaxEntity> taxDetails
    ) {
        SaleTaxCalculationResultDto result = new SaleTaxCalculationResultDto();
        result.DetailList = new ArrayList<>(details);
        result.TaxDetailList = new ArrayList<>(taxDetails);
        result.NumTotalPriceNoTax = details.stream()
                .map(detail -> amount(detail.NumPriceSubTotal))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        result.NumTotalTax = details.stream()
                .map(detail -> amount(detail.NumTotalTax))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        result.NumTotalPrice = details.stream()
                .map(detail -> amount(detail.NumTotalPrice))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return result;
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
