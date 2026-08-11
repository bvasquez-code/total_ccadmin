package com.ccadmin.app.sale.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface ISaleDeliveryOrderDto {

    String getSaleCod();
    String getPresaleCod();
    String getStoreCod();
    String getStoreName();
    Date getCreationDate();
    BigDecimal getNumTotalPrice();
    BigDecimal getNumTotalPaid();
    Long getPaymentCount();
    String getCurrencyCod();
    String getSaleStatus();
    String getIsPaid();
    String getDeliveryTypeCod();
    String getDeliveryTypeName();
    String getDeliveryStatus();
    String getAddress();
    Date getScheduledFrom();
    Date getScheduledTo();
    String getTrackingNumber();
}
