package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface IDocumentsReportDto {
    String getDocumentCod();
    String getCounterfoilCod();
    String getDocumentType();
    String getDocumentRole();
    String getOperationCod();
    Date getReportDate();
    String getStoreName();
    String getClientCod();
    String getCurrencyCod();
    String getDocumentStatus();
    BigDecimal getAmount();
    String getStoreCod();
}
