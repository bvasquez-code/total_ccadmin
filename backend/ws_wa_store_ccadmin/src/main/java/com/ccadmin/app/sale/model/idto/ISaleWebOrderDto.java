package com.ccadmin.app.sale.model.idto;

import java.math.BigDecimal;
import java.util.Date;

public interface ISaleWebOrderDto {

    String getSaleCod();
    String getPresaleCod();
    String getClientCod();
    String getClientName();
    BigDecimal getNumTotalPrice();
    Date getCreationDate();
    String getSaleStatus();
    String getIsPaid();
    String getHasFiscalDocument();
    String getHasCreditNote();
    String getDeliveryTypeCod();
    String getDeliveryTypeName();
    String getDeliveryStatus();
}
