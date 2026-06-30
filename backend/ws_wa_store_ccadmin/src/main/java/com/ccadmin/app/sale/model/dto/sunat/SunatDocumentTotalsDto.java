package com.ccadmin.app.sale.model.dto.sunat;

import java.math.BigDecimal;

public class SunatDocumentTotalsDto {
    public BigDecimal TaxableAmount = BigDecimal.ZERO;
    public BigDecimal ExoneratedAmount = BigDecimal.ZERO;
    public BigDecimal UnaffectedAmount = BigDecimal.ZERO;
    public BigDecimal FreeAmount = BigDecimal.ZERO;
    public BigDecimal TaxAmount = BigDecimal.ZERO;
    public BigDecimal DiscountTotal = BigDecimal.ZERO;
    public BigDecimal ChargeTotal = BigDecimal.ZERO;
    public BigDecimal LineExtensionAmount = BigDecimal.ZERO;
    public BigDecimal TaxInclusiveAmount = BigDecimal.ZERO;
    public BigDecimal PayableAmount = BigDecimal.ZERO;
}
