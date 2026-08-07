package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;

public final class CreditNoteHeadEntityFactory {

    private CreditNoteHeadEntityFactory() {
    }

    public static CreditNoteHeadEntity fromSale(
            CreditNoteHeadEntity source,
            SaleHeadEntity saleHead,
            String creditNoteStatus
    ) {
        source.SaleCod = saleHead.SaleCod;
        source.ClientCod = saleHead.ClientCod;
        source.CurrencyCod = saleHead.CurrencyCod;
        source.CurrencyCodSys = saleHead.CurrencyCodSys;
        source.PeriodId = saleHead.PeriodId;
        source.NumExchangevalue = saleHead.NumExchangevalue;
        source.CreditNoteStatus = creditNoteStatus;
        return source;
    }
}
