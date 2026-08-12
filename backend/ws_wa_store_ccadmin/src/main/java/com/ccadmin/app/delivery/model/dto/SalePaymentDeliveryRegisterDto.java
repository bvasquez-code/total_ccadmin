package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.model.entity.TrxPaymentDocumentEntity;

import java.util.ArrayList;
import java.util.List;

public class SalePaymentDeliveryRegisterDto {

    public String OrderToken;
    public TrxPaymentEntity TrxPayment;
    public List<TrxPaymentDocumentEntity> DocumentList = new ArrayList<>();
}
