package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;

public class CreditNoteDetWarehouseID implements Serializable {

    public String CreditNoteCod;
    public int ItemNumber;

    public CreditNoteDetWarehouseID(){

    }
    public CreditNoteDetWarehouseID(String creditNoteCod, int itemNumber) {
        CreditNoteCod = creditNoteCod;
        ItemNumber = itemNumber;
    }
}
