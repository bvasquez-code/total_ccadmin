package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class SaleTaxCalculationResultDto {

    public List<SaleDetEntity> DetailList = new ArrayList<>();
    public List<SaleDetTaxEntity> TaxDetailList = new ArrayList<>();
    public BigDecimal NumTotalPriceNoTax = BigDecimal.ZERO;
    public BigDecimal NumTotalTax = BigDecimal.ZERO;
    public BigDecimal NumTotalPrice = BigDecimal.ZERO;

    public void addLine(SaleDetEntity saleDet, List<SaleDetTaxEntity> taxDetailList) {
        this.DetailList.add(saleDet);
        this.TaxDetailList.addAll(taxDetailList);
    }

    public void recalculateTotals() {
        NumTotalPriceNoTax = DetailList.stream()
                .map(detail -> detail.NumPriceSubTotal == null ? BigDecimal.ZERO : detail.NumPriceSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        NumTotalTax = DetailList.stream()
                .map(detail -> detail.NumTotalTax == null ? BigDecimal.ZERO : detail.NumTotalTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        NumTotalPrice = DetailList.stream()
                .map(detail -> detail.NumTotalPrice == null ? BigDecimal.ZERO : detail.NumTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
