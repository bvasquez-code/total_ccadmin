package com.local.app.pinpad.model.dto;

import java.time.LocalDateTime;

public class PinpadPaymentAckDto {

    private Boolean centralSaved;
    private String centralPaymentCod;
    private LocalDateTime savedAt;
    private String message;

    public Boolean getCentralSaved() { return centralSaved; }
    public void setCentralSaved(Boolean centralSaved) { this.centralSaved = centralSaved; }
    public String getCentralPaymentCod() { return centralPaymentCod; }
    public void setCentralPaymentCod(String centralPaymentCod) { this.centralPaymentCod = centralPaymentCod; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
