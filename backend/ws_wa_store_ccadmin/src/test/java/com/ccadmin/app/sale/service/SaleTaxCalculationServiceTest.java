package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.CreditNoteTaxCalculationResultDto;
import com.ccadmin.app.sale.model.dto.SaleTaxCalculationResultDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.factory.SaleDetailSplitLineDtoFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaleTaxCalculationServiceTest {

    private final SaleTaxCalculationService saleTaxCalculationService = new SaleTaxCalculationService();

    @Test
    void splitsExistingTaxSnapshotAndAssignsRoundingResidualToLastLine() {
        SaleDetEntity originDetail = saleDetail();
        SaleDetTaxEntity originTax = saleTax();

        SaleTaxCalculationResultDto result = saleTaxCalculationService.splitExistingSaleDetail(
                originDetail,
                List.of(originTax),
                List.of(
                        SaleDetailSplitLineDtoFactory.fromPicking(1, 1, "L-01", null),
                        SaleDetailSplitLineDtoFactory.fromPicking(2, 2, "L-02", null)
                ),
                "ADMIN"
        );

        assertEquals(2, result.DetailList.size());
        assertEquals(new BigDecimal("33.33"), result.DetailList.get(0).NumTotalPrice);
        assertEquals(new BigDecimal("66.67"), result.DetailList.get(1).NumTotalPrice);
        assertEquals(new BigDecimal("1.00"), result.DetailList.get(0).NumDiscount);
        assertEquals(new BigDecimal("1.00"), result.DetailList.get(1).NumDiscount);
        assertEquals(new BigDecimal("5.08"), result.DetailList.get(0).NumTotalTax);
        assertEquals(new BigDecimal("10.17"), result.DetailList.get(1).NumTotalTax);
        assertEquals(new BigDecimal("100.00"), result.NumTotalPrice);
        assertEquals(new BigDecimal("15.25"), result.NumTotalTax);

        assertEquals(2, result.TaxDetailList.size());
        assertEquals(new BigDecimal("5.08"), result.TaxDetailList.get(0).TaxAmount);
        assertEquals(new BigDecimal("10.17"), result.TaxDetailList.get(1).TaxAmount);
        assertEquals(new BigDecimal("1.0000"), result.TaxDetailList.get(0).TaxQuantity);
        assertEquals(new BigDecimal("2.0000"), result.TaxDetailList.get(1).TaxQuantity);
    }

    @Test
    void buildsCreditNoteTaxLinesFromTheSameTaxCore() {
        SaleDetEntity originDetail = saleDetail();
        SaleDetTaxEntity originTax = saleTax();
        CreditNoteDetEntity creditNoteDetail = new CreditNoteDetEntity();
        creditNoteDetail.ItemNumber = 1;
        creditNoteDetail.NumUnit = 1;

        creditNoteDetail.NumTotalPrice = new BigDecimal("33.33");

        CreditNoteTaxCalculationResultDto result = saleTaxCalculationService.buildCreditNoteTaxResult(
                "NC001",
                creditNoteDetail,
                originDetail,
                List.of(originTax),
                "ADMIN"
        );

        assertEquals(1, result.TaxDetailList.size());
        assertEquals(new BigDecimal("28.25"), result.TaxDetailList.get(0).TaxBaseAmount);
        assertEquals(new BigDecimal("1.0000"), result.TaxDetailList.get(0).TaxQuantity);
        assertEquals(new BigDecimal("5.08"), result.TaxDetailList.get(0).TaxAmount);
        assertEquals(originTax.TaxCod, result.TaxDetailList.get(0).TaxCod);
        assertEquals("ADMIN", result.TaxDetailList.get(0).CreationUser);
        assertEquals(new BigDecimal("5.08"), result.NumTotalTax);
        assertEquals(new BigDecimal("28.25"), result.NumPriceSubTotal);
        assertEquals("S", result.IsAppliedTax);
    }

    private SaleDetEntity saleDetail() {
        SaleDetEntity detail = new SaleDetEntity();
        detail.SaleCod = "ST001";
        detail.ItemNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.NumUnit = 3;
        detail.NumUnitPrice = new BigDecimal("33.67");
        detail.NumDiscount = new BigDecimal("1.00");
        detail.NumUnitPriceSale = new BigDecimal("33.33");
        detail.NumTotalPrice = new BigDecimal("100.00");
        detail.NumPriceSubTotal = new BigDecimal("84.75");
        detail.NumTotalTax = new BigDecimal("15.25");
        detail.ProductUnitName = "NIU";
        detail.ProductUnitFactor = 1;
        detail.IsAppliedTax = "S";
        return detail;
    }

    private SaleDetTaxEntity saleTax() {
        SaleDetTaxEntity tax = new SaleDetTaxEntity();
        tax.SaleCod = "ST001";
        tax.ItemNumber = 1;
        tax.TaxLineNumber = 1;
        tax.TaxCod = "IGV";
        tax.TaxName = "IGV";
        tax.TaxCalculationType = "P";
        tax.IsInformative = "N";
        tax.TaxRateValue = new BigDecimal("18.0000");
        tax.FixedUnitAmount = new BigDecimal("0.0000");
        tax.TaxBaseAmount = new BigDecimal("84.75");
        tax.TaxQuantity = new BigDecimal("3.0000");
        tax.TaxAmount = new BigDecimal("15.25");
        tax.CalculationOrder = 1;
        return tax;
    }
}
