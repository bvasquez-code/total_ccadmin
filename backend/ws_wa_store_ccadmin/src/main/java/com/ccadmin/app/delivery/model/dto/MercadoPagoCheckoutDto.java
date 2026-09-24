package com.ccadmin.app.delivery.model.dto;

import java.math.BigDecimal;

public class MercadoPagoCheckoutDto {
    public String PublicKey;
    public String Locale;
    public String CurrencyCod;
    public BigDecimal Amount;
    public int MaxInstallments;
    public boolean TestMode;
    public MercadoPagoResultDto Payment;
}
