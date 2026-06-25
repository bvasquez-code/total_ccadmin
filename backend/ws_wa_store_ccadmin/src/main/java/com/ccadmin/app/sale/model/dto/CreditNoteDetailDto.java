package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.sale.model.entity.*;

import java.util.List;

public class CreditNoteDetailDto {

    public ClientEntity Client;

    public CreditNoteHeadEntity Headboard;
    public CreditNoteDocumentEntity Document;
    public SaleDocumentEntity DocumentReference;
    public List<CreditNoteDetDto> DetailList;
    public List<SalePaymentEntity> DetailPayment;

}
