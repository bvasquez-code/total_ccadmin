package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;

public class CreditNoteHeadDto {

    public CreditNoteHeadEntity CreditNoteHead;
    public ClientEntity Client;
    public CreditNoteDocumentEntity CreditNoteDocument;

    public CreditNoteHeadDto(){

    }

    public CreditNoteHeadDto(CreditNoteHeadEntity CreditNoteHead,ClientEntity Client,CreditNoteDocumentEntity CreditNoteDocument){
        this.CreditNoteHead = CreditNoteHead;
        this.Client = Client;
        this.CreditNoteDocument = CreditNoteDocument;
    }

}
