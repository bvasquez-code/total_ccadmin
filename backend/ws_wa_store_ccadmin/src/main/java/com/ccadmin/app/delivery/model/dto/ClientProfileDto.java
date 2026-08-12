package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.delivery.model.idto.IClientProfileDto;

public class ClientProfileDto {

    public Long ClientAccountID;
    public String ClientCod;
    public String Email;
    public String DocumentType;
    public String DocumentNumber;
    public String Names;
    public String LastNames;
    public String Phone;

    public static ClientProfileDto from(IClientProfileDto source) {
        ClientProfileDto result = new ClientProfileDto();
        result.ClientAccountID = source.getClientAccountID();
        result.ClientCod = source.getClientCod();
        result.Email = source.getEmail();
        result.DocumentType = source.getDocumentType();
        result.DocumentNumber = source.getDocumentNumber();
        result.Names = source.getNames();
        result.LastNames = source.getLastNames();
        result.Phone = source.getPhone();
        return result;
    }
}
