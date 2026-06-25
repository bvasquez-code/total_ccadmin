package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;

public class SaleDocumentID implements Serializable {

    public String DocumentCod;
    public String CounterfoilCod;

    public SaleDocumentID(){

    }

    public SaleDocumentID(String documentCod, String counterfoilCod) {
        DocumentCod = documentCod;
        CounterfoilCod = counterfoilCod;
    }
}
