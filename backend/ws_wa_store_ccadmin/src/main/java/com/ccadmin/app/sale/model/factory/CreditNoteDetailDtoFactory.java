package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.sale.model.dto.CreditNoteDetDto;
import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.entity.CreditNoteApplicationEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;

import java.math.BigDecimal;
import java.util.List;

public final class CreditNoteDetailDtoFactory {

    private CreditNoteDetailDtoFactory() {
    }

    public static CreditNoteDetailDto fromEntities(
            CreditNoteHeadEntity head,
            ClientEntity client,
            List<CreditNoteDetDto> details,
            List<SalePaymentEntity> payments,
            List<CreditNoteApplicationEntity> applications,
            BigDecimal availableBalance,
            CreditNoteDocumentEntity document,
            SaleDocumentEntity documentReference
    ) {
        CreditNoteDetailDto result = new CreditNoteDetailDto();
        result.Headboard = head;
        result.Client = client;
        result.DetailList = details;
        result.DetailPayment = payments;
        result.ApplicationList = applications;
        result.NumAvailableBalance = availableBalance;
        result.Document = document;
        result.DocumentReference = documentReference;
        return result;
    }
}
