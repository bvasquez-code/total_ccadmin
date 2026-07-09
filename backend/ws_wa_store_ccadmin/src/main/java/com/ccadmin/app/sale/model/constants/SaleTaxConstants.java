package com.ccadmin.app.sale.model.constants;

import java.math.BigDecimal;

public final class SaleTaxConstants {

    public static final String YES = "S";
    public static final String NO = "N";
    public static final String TAX_CALCULATION_PERCENT = "P";
    public static final String TAX_CALCULATION_FIXED = "F";
    public static final String IGV_TAX_COD = "1000";
    public static final String TAXED_AFFECTATION_COD = "10";
    public static final BigDecimal STANDARD_IGV_RATE = new BigDecimal("18.0000");

    private SaleTaxConstants() {
    }
}
