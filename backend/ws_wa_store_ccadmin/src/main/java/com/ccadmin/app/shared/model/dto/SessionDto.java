package com.ccadmin.app.shared.model.dto;

public class SessionDto {

    public Long SessionID;
    public Long CashSessionID;
    public String UserCod;
    public String Email;
    public String Name;
    public String StoreCod;

    public SessionDto()
    {
        this.SessionID = null;
        this.CashSessionID = null;
        this.UserCod = "";
        this.Email = "";
        this.Name = "";
        this.StoreCod = "";
    }

}
