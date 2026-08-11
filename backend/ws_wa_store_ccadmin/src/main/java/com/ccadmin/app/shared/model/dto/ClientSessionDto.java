package com.ccadmin.app.shared.model.dto;

public class ClientSessionDto {

    public final Long ClientAccountID;
    public final String ClientCod;
    public final String Email;
    public final String Names;

    public ClientSessionDto(Long ClientAccountID, String ClientCod, String Email, String Names) {
        this.ClientAccountID = ClientAccountID;
        this.ClientCod = ClientCod;
        this.Email = Email;
        this.Names = Names;
    }
}
