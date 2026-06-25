package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;

public class SalePaymentDto {

    public SalePaymentEntity SalePayment;
    public TrxPaymentEntity TrxPayment;

    public SalePaymentDto(SalePaymentEntity salePayment, TrxPaymentEntity trxPayment) {
        SalePayment = salePayment;
        TrxPayment = trxPayment;
    }
}
