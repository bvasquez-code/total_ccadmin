package com.ccadmin.app.sunat.model.dto.sunat;

import java.math.BigDecimal;

public class SunatDocumentLineDto {
    public int ItemNumber;
    public String ProductCode;
    public String Description;
    public String UnitCode = "NIU";
    public BigDecimal Quantity;
    public BigDecimal UnitPrice;
    public BigDecimal PriceAmount;
    public String PriceTypeCode = "01";
    public BigDecimal LineExtensionAmount;
    public BigDecimal TaxableAmount;
    public BigDecimal TaxAmount;
    public BigDecimal TaxPercent = BigDecimal.ZERO;
    public String TaxCategoryCode = "";
    public String TaxExemptionReasonCode = "";
    public String TaxSchemeId = "";
    public String TaxSchemeName = "";
    public String TaxTypeCode = "";
}
