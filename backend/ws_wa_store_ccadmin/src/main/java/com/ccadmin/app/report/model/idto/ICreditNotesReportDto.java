package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface ICreditNotesReportDto {
    String getCreditNoteCod();
    String getSaleCod();
    Date getReportDate();
    String getStoreName();
    String getClientName();
    String getCreditNoteStatus();
    String getReason();
    String getCurrencyCod();
    BigDecimal getAmountNoTax();
    BigDecimal getTax();
    BigDecimal getAmount();
    Long getQuantity();
    Long getReturnedQuantity();
    String getIsStockReturned();
    String getIsPaid();
    String getStoreCod();
}
