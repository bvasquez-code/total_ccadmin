package com.ccadmin.app.sunat.model.dto;

public class SunatPendingOperationDto {
    public String Operation;
    public String RequiredPhase;
    public String Message;

    public SunatPendingOperationDto(String operation, String requiredPhase, String message) {
        Operation = operation;
        RequiredPhase = requiredPhase;
        Message = message;
    }
}
