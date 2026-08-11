package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.idto.ISaleDeliveryOrderDto;

import java.math.BigDecimal;
import java.util.Date;

public class SaleDeliveryOrderDto {

    public String SaleCod;
    public String PresaleCod;
    public String StoreCod;
    public String StoreName;
    public Date CreationDate;
    public BigDecimal NumTotalPrice;
    public BigDecimal NumTotalPaid;
    public long PaymentCount;
    public String CurrencyCod;
    public String SaleStatus;
    public String IsPaid;
    public String DeliveryTypeCod;
    public String DeliveryTypeName;
    public String DeliveryStatus;
    public String Address;
    public Date ScheduledFrom;
    public Date ScheduledTo;
    public String TrackingNumber;
    public boolean CanResumePayment;
    public String OrderToken;

    public static SaleDeliveryOrderDto from(ISaleDeliveryOrderDto source) {
        SaleDeliveryOrderDto result = new SaleDeliveryOrderDto();
        result.SaleCod = source.getSaleCod();
        result.PresaleCod = source.getPresaleCod();
        result.StoreCod = source.getStoreCod();
        result.StoreName = source.getStoreName();
        result.CreationDate = source.getCreationDate();
        result.NumTotalPrice = source.getNumTotalPrice();
        result.NumTotalPaid = source.getNumTotalPaid();
        result.PaymentCount = source.getPaymentCount() == null ? 0 : source.getPaymentCount();
        result.CurrencyCod = source.getCurrencyCod();
        result.SaleStatus = source.getSaleStatus();
        result.IsPaid = source.getIsPaid();
        result.DeliveryTypeCod = source.getDeliveryTypeCod();
        result.DeliveryTypeName = source.getDeliveryTypeName();
        result.DeliveryStatus = source.getDeliveryStatus();
        result.Address = source.getAddress();
        result.ScheduledFrom = source.getScheduledFrom();
        result.ScheduledTo = source.getScheduledTo();
        result.TrackingNumber = source.getTrackingNumber();
        result.CanResumePayment = SaleConstants.PENDING.equals(result.SaleStatus)
                && !"S".equals(result.IsPaid)
                && result.PaymentCount == 0
                && !SaleConstants.DELIVERY_STATUS_CANCELLED.equals(result.DeliveryStatus)
                && !SaleConstants.DELIVERY_STATUS_FAILED.equals(result.DeliveryStatus);
        return result;
    }
}
