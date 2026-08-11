package com.ccadmin.app.delivery.model.dto;

public class ClientLoginResponseDto {

    public final String Token;
    public final Long ClientAccountID;
    public final String ClientCod;
    public final String Email;
    public final String Names;

    public ClientLoginResponseDto(
            String Token,
            Long ClientAccountID,
            String ClientCod,
            String Email,
            String Names
    ) {
        this.Token = Token;
        this.ClientAccountID = ClientAccountID;
        this.ClientCod = ClientCod;
        this.Email = Email;
        this.Names = Names;
    }
}
