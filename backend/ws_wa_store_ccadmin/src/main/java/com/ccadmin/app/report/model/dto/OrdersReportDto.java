package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IOrdersReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class OrdersReportDto {
    public String PresaleCod;
    public Date ReportDate;
    public String StoreName;
    public String ClientName;
    public String ChannelCod;
    public String OrderStatus;
    public String IsPaid;
    public String SaleCod;
    public String SaleStatus;
    public String DeliveryStatus;
    public String TrackingNumber;
    public String CurrencyCod;
    public BigDecimal Amount;
    public String StoreCod;

    public OrdersReportDto(IOrdersReportDto row) {
        this.PresaleCod = row.getPresaleCod();
        this.ReportDate = row.getReportDate();
        this.StoreName = row.getStoreName();
        this.ClientName = row.getClientName();
        this.ChannelCod = row.getChannelCod();
        this.OrderStatus = row.getOrderStatus();
        this.IsPaid = row.getIsPaid();
        this.SaleCod = row.getSaleCod();
        this.SaleStatus = row.getSaleStatus();
        this.DeliveryStatus = row.getDeliveryStatus();
        this.TrackingNumber = row.getTrackingNumber();
        this.CurrencyCod = row.getCurrencyCod();
        this.Amount = row.getAmount();
        this.StoreCod = row.getStoreCod();
    }
}
