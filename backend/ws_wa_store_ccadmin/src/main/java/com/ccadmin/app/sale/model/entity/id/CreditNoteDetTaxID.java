package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;

public class CreditNoteDetTaxID implements Serializable {

    public String CreditNoteCod;
    public int ItemNumber;
    public int TaxLineNumber;

    public CreditNoteDetTaxID() {
    }

    public CreditNoteDetTaxID(String creditNoteCod, int itemNumber, int taxLineNumber) {
        CreditNoteCod = creditNoteCod;
        ItemNumber = itemNumber;
        TaxLineNumber = taxLineNumber;
    }
}
