package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;

import java.math.BigDecimal;

public class PresaleCancellationDetailDto {

    public PresaleHeadEntity Headboard;
    public SaleDetailDto SaleDetail;
    public boolean HasStockReservation;
    public BigDecimal PendingPaymentAmount;

    public PresaleCancellationDetailDto() {
        this.PendingPaymentAmount = BigDecimal.ZERO;
    }
}
