package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;

public class CreditNoteDetID implements Serializable {

    public String CreditNoteCod;
    public int ItemNumber;

    public CreditNoteDetID(){

    }

    public CreditNoteDetID(String creditNoteCod, int itemNumber) {
        CreditNoteCod = creditNoteCod;
        ItemNumber = itemNumber;
    }
}
