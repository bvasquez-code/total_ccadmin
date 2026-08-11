package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.PeriodEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.system.model.entity.CurrencyEntity;

import java.math.BigDecimal;

public final class PresaleHeadEntityFactory {

    private PresaleHeadEntityFactory() {
    }

    public static PresaleHeadEntity fromSaveRequest(
            PresaleHeadEntity source,
            PeriodEntity period,
            CurrencyEntity systemCurrency,
            CurrencyEntity selectedCurrency,
            String storeCod,
            String saleStatus
    ) {
        source.PeriodId = period.PeriodId;
        source.CurrencyCodSys = systemCurrency.CurrencyCod;
        source.NumExchangevalue = BigDecimal.ONE;
        if (!source.CurrencyCodSys.equals(source.CurrencyCod)) {
            source.NumExchangevalue = selectedCurrency.NumExchangevalue;
        }
        source.StoreCod = storeCod;
        source.SaleStatus = saleStatus;
        if (source.IsPaid == null || source.IsPaid.isBlank()) {
            source.IsPaid = "N";
        }
        if (!source.existClient()) {
            source.ClientCod = null;
        }
        return source;
    }
}
