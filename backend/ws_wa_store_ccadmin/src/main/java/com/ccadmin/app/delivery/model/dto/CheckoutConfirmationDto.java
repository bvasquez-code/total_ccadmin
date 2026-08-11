package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.sale.model.dto.SaleDetailDto;

public class CheckoutConfirmationDto {

    public String OrderToken;
    public SaleDetailDto SaleDetail;

    public CheckoutConfirmationDto(String orderToken, SaleDetailDto saleDetail) {
        OrderToken = orderToken;
        SaleDetail = saleDetail;
    }
}
