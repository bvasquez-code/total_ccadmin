package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.sale.model.dto.CreditNoteHeadDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;

public final class CreditNoteHeadDtoFactory {

    private CreditNoteHeadDtoFactory() {
    }

    public static CreditNoteHeadDto fromEntities(
            CreditNoteHeadEntity head,
            ClientEntity client,
            CreditNoteDocumentEntity document
    ) {
        CreditNoteHeadDto result = new CreditNoteHeadDto();
        result.CreditNoteHead = head;
        result.Client = client;
        result.CreditNoteDocument = document;
        return result;
    }
}
