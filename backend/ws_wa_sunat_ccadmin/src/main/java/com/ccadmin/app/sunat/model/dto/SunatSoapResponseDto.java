package com.ccadmin.app.sunat.model.dto;

public class SunatSoapResponseDto {
    public int HttpStatusCode;
    public String Endpoint;
    public String RawResponse;
    public String FaultCode;
    public String FaultString;
    public String ApplicationResponseBase64;
    public String Ticket;
    public String ContentBase64;

    public boolean hasFault() {
        return FaultCode != null && !FaultCode.isBlank();
    }

    public boolean hasApplicationResponse() {
        return ApplicationResponseBase64 != null && !ApplicationResponseBase64.isBlank();
    }

    public boolean hasTicket() {
        return Ticket != null && !Ticket.isBlank();
    }

    public boolean hasContent() {
        return ContentBase64 != null && !ContentBase64.isBlank();
    }
}
