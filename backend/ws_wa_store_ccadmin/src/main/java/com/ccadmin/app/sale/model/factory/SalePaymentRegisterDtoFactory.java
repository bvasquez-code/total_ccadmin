package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;

public final class SalePaymentRegisterDtoFactory {

    private SalePaymentRegisterDtoFactory() {
    }

    public static SalePaymentRegisterDto fromTransaction(
            String saleCod,
            long trxPaymentId
    ) {
        SalePaymentRegisterDto result = new SalePaymentRegisterDto();
        result.SaleCod = saleCod;
        result.TrxPaymentId = trxPaymentId;
        return result;
    }
}
