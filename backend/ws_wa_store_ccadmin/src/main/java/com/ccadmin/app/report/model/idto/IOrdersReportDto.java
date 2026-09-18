package com.ccadmin.app.report.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface IOrdersReportDto {
    String getPresaleCod();
    Date getReportDate();
    String getStoreName();
    String getClientName();
    String getChannelCod();
    String getOrderStatus();
    String getIsPaid();
    String getSaleCod();
    String getSaleStatus();
    String getDeliveryStatus();
    String getTrackingNumber();
    String getCurrencyCod();
    BigDecimal getAmount();
    String getStoreCod();
}
