package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;
import java.util.Objects;

public class SaleDocumentID implements Serializable {

    public String DocumentCod;
    public String SaleCod;

    public SaleDocumentID(){

    }

    public SaleDocumentID(String documentCod, String saleCod) {
        DocumentCod = documentCod;
        SaleCod = saleCod;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SaleDocumentID that)) return false;
        return Objects.equals(DocumentCod, that.DocumentCod)
                && Objects.equals(SaleCod, that.SaleCod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(DocumentCod, SaleCod);
    }
}
