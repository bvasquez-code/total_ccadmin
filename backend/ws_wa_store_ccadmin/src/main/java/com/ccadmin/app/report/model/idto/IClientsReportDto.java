package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface IClientsReportDto {
    String getClientCod();
    String getClientName();
    String getDocumentNum();
    String getStoreName();
    String getCurrencyCod();
    Long getSaleCount();
    Date getFirstSaleDate();
    Date getLastSaleDate();
    BigDecimal getAmount();
    BigDecimal getAverageTicket();
    String getStoreCod();
}
