package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.CreditNoteApplicationEntity;

import java.math.BigDecimal;

public final class CreditNoteApplicationEntityFactory {

    private CreditNoteApplicationEntityFactory() {
    }

    public static CreditNoteApplicationEntity fromApplication(
            String creditNoteCod,
            String saleCod,
            long trxPaymentId,
            BigDecimal amountApplied
    ) {
        CreditNoteApplicationEntity application = new CreditNoteApplicationEntity();
        application.CreditNoteCod = creditNoteCod;
        application.SaleCod = saleCod;
        application.TrxPaymentId = trxPaymentId;
        application.AmountApplied = amountApplied;
        return application;
    }
}
