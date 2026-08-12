package com.ccadmin.app.sale.model.dto;

import com.ccadmin.app.sale.model.idto.ISaleWebOrderDto;

import java.math.BigDecimal;
import java.util.Date;

public class SaleWebOrderDto {

    public String SaleCod;
    public String PresaleCod;
    public String ClientCod;
    public String ClientName;
    public BigDecimal NumTotalPrice;
    public Date CreationDate;
    public String SaleStatus;
    public String IsPaid;
    public String HasFiscalDocument;
    public String HasCreditNote;
    public String DeliveryTypeCod;
    public String DeliveryTypeName;
    public String DeliveryStatus;

    public static SaleWebOrderDto from(ISaleWebOrderDto source) {
        SaleWebOrderDto result = new SaleWebOrderDto();
        result.SaleCod = source.getSaleCod();
        result.PresaleCod = source.getPresaleCod();
        result.ClientCod = source.getClientCod();
        result.ClientName = source.getClientName();
        result.NumTotalPrice = source.getNumTotalPrice();
        result.CreationDate = source.getCreationDate();
        result.SaleStatus = source.getSaleStatus();
        result.IsPaid = source.getIsPaid();
        result.HasFiscalDocument = source.getHasFiscalDocument();
        result.HasCreditNote = source.getHasCreditNote();
        result.DeliveryTypeCod = source.getDeliveryTypeCod();
        result.DeliveryTypeName = source.getDeliveryTypeName();
        result.DeliveryStatus = source.getDeliveryStatus();
        return result;
    }
}
