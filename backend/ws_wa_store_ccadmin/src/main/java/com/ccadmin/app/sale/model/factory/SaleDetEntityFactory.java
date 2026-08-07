package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.SaleDetailSplitLineDto;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;

import java.math.BigDecimal;

public final class SaleDetEntityFactory {

    private SaleDetEntityFactory() {
    }

    public static SaleDetEntity fromPresale(
            PresaleDetEntity presaleDetail,
            String saleCod,
            BigDecimal priceSubtotal,
            BigDecimal totalTax,
            String isAppliedTax
    ) {
        SaleDetEntity detail = new SaleDetEntity();
        detail.SaleCod = saleCod;
        detail.ItemNumber = presaleDetail.ItemNumber;
        detail.ProductCod = presaleDetail.ProductCod;
        detail.Variant = presaleDetail.Variant;
        detail.NumUnit = presaleDetail.NumUnit;
        detail.NumUnitPrice = presaleDetail.NumUnitPrice;
        detail.NumDiscount = presaleDetail.NumDiscount;
        detail.NumUnitPriceSale = presaleDetail.NumUnitPriceSale;
        detail.NumTotalPrice = presaleDetail.NumTotalPrice;
        detail.NumPriceSubTotal = priceSubtotal;
        detail.NumTotalTax = totalTax;
        detail.ProductUnitName = presaleDetail.ProductUnitName;
        detail.ProductUnitFactor = presaleDetail.ProductUnitFactor;
        detail.IsDigital = presaleDetail.IsDigital;
        detail.IsAppliedTax = isAppliedTax;
        detail.LotNumber = presaleDetail.LotNumber;
        detail.ExpirationDate = presaleDetail.ExpirationDate;
        return detail;
    }

    public static SaleDetEntity copyForItem(SaleDetEntity source, int itemNumber) {
        SaleDetEntity detail = copyBase(source, itemNumber);
        detail.NumUnit = source.NumUnit;
        detail.NumTotalPrice = source.NumTotalPrice;
        detail.NumPriceSubTotal = source.NumPriceSubTotal;
        detail.NumTotalTax = source.NumTotalTax;
        detail.LotNumber = source.LotNumber;
        detail.ExpirationDate = source.ExpirationDate;
        return detail;
    }

    public static SaleDetEntity fromSplit(
            SaleDetEntity source,
            SaleDetailSplitLineDto splitLine,
            BigDecimal totalPrice,
            BigDecimal priceSubtotal,
            BigDecimal totalTax
    ) {
        SaleDetEntity detail = copyBase(source, splitLine.ItemNumber);
        detail.NumUnit = splitLine.NumUnit;
        detail.NumTotalPrice = totalPrice;
        detail.NumPriceSubTotal = priceSubtotal;
        detail.NumTotalTax = totalTax;
        detail.LotNumber = splitLine.LotNumber;
        detail.ExpirationDate = splitLine.ExpirationDate;
        return detail;
    }

    private static SaleDetEntity copyBase(SaleDetEntity source, int itemNumber) {
        SaleDetEntity detail = new SaleDetEntity();
        detail.SaleCod = source.SaleCod;
        detail.ItemNumber = itemNumber;
        detail.ProductCod = source.ProductCod;
        detail.Variant = source.Variant;
        detail.NumUnitPrice = source.NumUnitPrice;
        detail.NumDiscount = source.NumDiscount;
        detail.NumUnitPriceSale = source.NumUnitPriceSale;
        detail.ProductUnitName = source.ProductUnitName;
        detail.ProductUnitFactor = source.ProductUnitFactor;
        detail.IsDigital = source.IsDigital;
        detail.IsAppliedTax = source.IsAppliedTax;
        detail.CreationUser = source.CreationUser;
        detail.CreationDate = source.CreationDate;
        detail.ModifyUser = source.ModifyUser;
        detail.ModifyDate = source.ModifyDate;
        detail.Status = source.Status;
        return detail;
    }
}
