package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.PresaleCancellationDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;

import java.math.BigDecimal;

public final class PresaleCancellationDetailDtoFactory {

    private PresaleCancellationDetailDtoFactory() {
    }

    public static PresaleCancellationDetailDto fromPresale(
            PresaleHeadEntity presaleHead,
            boolean hasStockReservation,
            SaleDetailDto saleDetail,
            BigDecimal pendingPaymentAmount
    ) {
        PresaleCancellationDetailDto result = new PresaleCancellationDetailDto();
        result.Headboard = presaleHead;
        result.HasStockReservation = hasStockReservation;
        result.SaleDetail = saleDetail;
        result.PendingPaymentAmount = pendingPaymentAmount == null
                ? BigDecimal.ZERO
                : pendingPaymentAmount;
        return result;
    }
}
