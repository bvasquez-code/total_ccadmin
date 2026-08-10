package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.*;

import java.util.List;

public final class SaleDetailDtoFactory {

    private SaleDetailDtoFactory() {
    }

    public static SaleDetailDto fromEntities(
            SaleHeadEntity head,
            List<SaleDetEntity> details,
            List<SalePaymentEntity> payments,
            List<SaleDocumentEntity> documents,
            CreditNoteDetailDto creditNoteDetail,
            SaleChannelEntity saleChannel
    ) {
        SaleDetailDto result = new SaleDetailDto();
        result.Headboard = head;
        result.DetailList = details;
        result.DetailPayment = payments;
        result.SaleDocumentList = documents;
        result.SaleDocument = documents.stream().findFirst().orElse(null);
        result.CreditNoteDetail = creditNoteDetail;
        result.SaleChannel = saleChannel;
        return result;
    }
}
