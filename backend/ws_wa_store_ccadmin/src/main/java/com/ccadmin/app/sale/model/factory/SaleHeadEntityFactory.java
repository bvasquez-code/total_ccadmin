package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.PeriodEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;

public final class SaleHeadEntityFactory {

    private SaleHeadEntityFactory() {
    }

    public static SaleHeadEntity fromPresale(
            PresaleHeadEntity presaleHead,
            PeriodEntity period,
            String saleCod,
            String saleStatus
    ) {
        SaleHeadEntity saleHead = new SaleHeadEntity();
        saleHead.SaleCod = saleCod;
        saleHead.PresaleCod = presaleHead.PresaleCod;
        saleHead.StoreCod = presaleHead.StoreCod;
        saleHead.ClientCod = presaleHead.ClientCod;
        saleHead.NumPriceSubTotal = presaleHead.NumPriceSubTotal;
        saleHead.NumDiscount = presaleHead.NumDiscount;
        saleHead.NumTotalPrice = presaleHead.NumTotalPrice;
        saleHead.NumTotalPriceNoTax = presaleHead.NumTotalPriceNoTax;
        saleHead.NumTotalTax = presaleHead.NumTotalTax;
        saleHead.Commenter = presaleHead.Commenter;
        saleHead.PeriodId = period.PeriodId;
        saleHead.SaleStatus = saleStatus;
        saleHead.CurrencyCod = presaleHead.CurrencyCod;
        saleHead.CurrencyCodSys = presaleHead.CurrencyCodSys;
        saleHead.NumExchangevalue = presaleHead.NumExchangevalue;
        saleHead.IsPaid = presaleHead.IsPaid;
        saleHead.HasCreditNote = "N";
        saleHead.HasFiscalDocument = "N";
        saleHead.IsPickingConfirmed = "N";
        return saleHead;
    }
}
