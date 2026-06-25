package com.ccadmin.app.sale.model.entity.id;

public class CreditNoteDocumentID {

    public String DocumentCod;
    public String CounterfoilCod;

    public CreditNoteDocumentID(){

    }

    public CreditNoteDocumentID(String documentCod, String counterfoilCod) {
        DocumentCod = documentCod;
        CounterfoilCod = counterfoilCod;
    }
}
