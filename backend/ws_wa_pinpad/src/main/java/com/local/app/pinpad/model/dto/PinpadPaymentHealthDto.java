package com.local.app.pinpad.model.dto;

public class PinpadPaymentHealthDto {

    private String status;
    private String agentVersion;
    private String pinpadStatus;
    private boolean simulatorEnabled;
    private String terminalId;
    private String merchantId;
    private String activePaymentId;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String agentVersion) { this.agentVersion = agentVersion; }
    public String getPinpadStatus() { return pinpadStatus; }
    public void setPinpadStatus(String pinpadStatus) { this.pinpadStatus = pinpadStatus; }
    public boolean isSimulatorEnabled() { return simulatorEnabled; }
    public void setSimulatorEnabled(boolean simulatorEnabled) { this.simulatorEnabled = simulatorEnabled; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getActivePaymentId() { return activePaymentId; }
    public void setActivePaymentId(String activePaymentId) { this.activePaymentId = activePaymentId; }
}
