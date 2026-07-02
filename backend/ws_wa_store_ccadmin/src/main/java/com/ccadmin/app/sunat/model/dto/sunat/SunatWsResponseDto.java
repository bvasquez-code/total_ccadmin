package com.ccadmin.app.sunat.model.dto.sunat;

public class SunatWsResponseDto {
    public String Status;
    public String Message;
    public Object Data;
    public boolean ErrorStatus;


    public static SunatWsResponseDto alert(String message){
        SunatWsResponseDto dto = new SunatWsResponseDto();
        dto.Status = "ERROR DE VALIDACIÓN INTERNA";
        dto.Message = message;
        dto.ErrorStatus = true;
        dto.Data = "ERROR DE VALIDACIÓN INTERNA : "+message;
        return dto;
    }
}
