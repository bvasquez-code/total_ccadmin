package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;

public class SaleDetTaxID implements Serializable {
    public String SaleCod;
    public int ItemNumber;
    public int TaxLineNumber;

    public SaleDetTaxID() {
    }

    public SaleDetTaxID(String saleCod, int itemNumber, int taxLineNumber) {
        SaleCod = saleCod;
        ItemNumber = itemNumber;
        TaxLineNumber = taxLineNumber;
    }
}
