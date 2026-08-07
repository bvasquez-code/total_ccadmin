package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.TaxAffectationEntity;
import com.ccadmin.app.sale.model.entity.TaxEntity;

import java.math.BigDecimal;

public final class SaleDetTaxEntityFactory {

    private SaleDetTaxEntityFactory() {
    }

    public static SaleDetTaxEntity fromTaxConfiguration(
            PresaleDetEntity presaleDetail,
            String saleCod,
            ProductTaxConfigEntity config,
            TaxEntity tax,
            TaxAffectationEntity affectation,
            BigDecimal taxRateValue,
            BigDecimal fixedUnitAmount,
            BigDecimal taxBaseAmount,
            BigDecimal taxQuantity,
            BigDecimal taxAmount
    ) {
        SaleDetTaxEntity line = new SaleDetTaxEntity();
        line.SaleCod = saleCod;
        line.ItemNumber = presaleDetail.ItemNumber;
        line.TaxCod = config.TaxCod;
        line.SunatTaxCod = tax.SunatTaxCod;
        line.TaxName = tax.Name;
        line.TaxAffectationCod = affectation == null ? null : affectation.TaxAffectationCod;
        line.TaxAffectationName = affectation == null ? null : affectation.Name;
        line.TaxCalculationType = config.TaxCalculationType;
        line.IsInformative = config.IsInformative;
        line.TaxRateValue = taxRateValue;
        line.FixedUnitAmount = fixedUnitAmount;
        line.TaxBaseAmount = taxBaseAmount;
        line.TaxQuantity = taxQuantity;
        line.TaxAmount = taxAmount;
        line.CalculationOrder = config.CalculationOrder;
        return line;
    }

    public static SaleDetTaxEntity copyForItem(
            SaleDetTaxEntity source,
            int itemNumber
    ) {
        return fromSplit(
                source,
                itemNumber,
                source.TaxBaseAmount,
                source.TaxQuantity,
                source.TaxAmount
        );
    }

    public static SaleDetTaxEntity fromSplit(
            SaleDetTaxEntity source,
            int itemNumber,
            BigDecimal taxBaseAmount,
            BigDecimal taxQuantity,
            BigDecimal taxAmount
    ) {
        SaleDetTaxEntity line = new SaleDetTaxEntity();
        line.SaleCod = source.SaleCod;
        line.ItemNumber = itemNumber;
        line.TaxLineNumber = source.TaxLineNumber;
        line.TaxCod = source.TaxCod;
        line.SunatTaxCod = source.SunatTaxCod;
        line.TaxName = source.TaxName;
        line.TaxAffectationCod = source.TaxAffectationCod;
        line.TaxAffectationName = source.TaxAffectationName;
        line.TaxCalculationType = source.TaxCalculationType;
        line.IsInformative = source.IsInformative;
        line.TaxRateValue = source.TaxRateValue;
        line.FixedUnitAmount = source.FixedUnitAmount;
        line.TaxBaseAmount = taxBaseAmount;
        line.TaxQuantity = taxQuantity;
        line.TaxAmount = taxAmount;
        line.CalculationOrder = source.CalculationOrder;
        line.CreationUser = source.CreationUser;
        line.CreationDate = source.CreationDate;
        line.ModifyUser = source.ModifyUser;
        line.ModifyDate = source.ModifyDate;
        line.Status = source.Status;
        return line;
    }
}
