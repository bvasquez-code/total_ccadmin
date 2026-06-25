package com.local.app.pinpad.model.dto;

import com.local.app.pinpad.enums.PinpadPaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PinpadPaymentRegisterDto {

    @NotBlank
    private String paymentId;
    private String saleCod;
    private BigDecimal amount;
    @NotNull
    @Positive
    private Long amountCents;
    @NotBlank
    private String currency;
    @NotNull
    private PinpadPaymentMethod paymentMethod;
    private String internalPaymentCode;
    private String cashier;
    private String storeCod;
    private String terminalCod;
    private String externalReference;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getSaleCod() {
        return saleCod;
    }

    public void setSaleCod(String saleCod) {
        this.saleCod = saleCod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PinpadPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PinpadPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getInternalPaymentCode() {
        return internalPaymentCode;
    }

    public void setInternalPaymentCode(String internalPaymentCode) {
        this.internalPaymentCode = internalPaymentCode;
    }

    public String getCashier() {
        return cashier;
    }

    public void setCashier(String cashier) {
        this.cashier = cashier;
    }

    public String getStoreCod() {
        return storeCod;
    }

    public void setStoreCod(String storeCod) {
        this.storeCod = storeCod;
    }

    public String getTerminalCod() {
        return terminalCod;
    }

    public void setTerminalCod(String terminalCod) {
        this.terminalCod = terminalCod;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }
}
