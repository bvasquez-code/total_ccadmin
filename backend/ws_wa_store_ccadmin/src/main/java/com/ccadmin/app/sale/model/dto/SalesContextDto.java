package com.ccadmin.app.sale.model.dto;

public class SalesContextDto {

    public final String StoreCod;
    public final String UserCod;
    public final Long CashSessionID;

    public SalesContextDto(String StoreCod, String UserCod, Long CashSessionID) {
        this.StoreCod = StoreCod;
        this.UserCod = UserCod;
        this.CashSessionID = CashSessionID;
    }
}
