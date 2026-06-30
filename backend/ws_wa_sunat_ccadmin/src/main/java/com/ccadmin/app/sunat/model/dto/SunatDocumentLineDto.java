package com.ccadmin.app.sunat.model.dto;

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
    public BigDecimal TaxPercent = BigDecimal.valueOf(18);
    public String TaxCategoryCode = "S";
    public String TaxExemptionReasonCode = "10";
    public String TaxSchemeId = "1000";
    public String TaxSchemeName = "IGV";
    public String TaxTypeCode = "VAT";
}
