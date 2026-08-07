package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.sale.model.dto.SalePaymentDto;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;

public final class SalePaymentDtoFactory {

    private SalePaymentDtoFactory() {
    }

    public static SalePaymentDto fromEntities(
            SalePaymentEntity salePayment,
            TrxPaymentEntity trxPayment
    ) {
        SalePaymentDto result = new SalePaymentDto();
        result.SalePayment = salePayment;
        result.TrxPayment = trxPayment;
        return result;
    }
}
