package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IClientsReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class ClientsReportDto {
    public String ClientCod;
    public String ClientName;
    public String DocumentNum;
    public String StoreName;
    public String CurrencyCod;
    public Long SaleCount;
    public Date FirstSaleDate;
    public Date LastSaleDate;
    public BigDecimal Amount;
    public BigDecimal AverageTicket;
    public String StoreCod;

    public ClientsReportDto(IClientsReportDto row) {
        this.ClientCod = row.getClientCod();
        this.ClientName = row.getClientName();
        this.DocumentNum = row.getDocumentNum();
        this.StoreName = row.getStoreName();
        this.CurrencyCod = row.getCurrencyCod();
        this.SaleCount = row.getSaleCount();
        this.FirstSaleDate = row.getFirstSaleDate();
        this.LastSaleDate = row.getLastSaleDate();
        this.Amount = row.getAmount();
        this.AverageTicket = row.getAverageTicket();
        this.StoreCod = row.getStoreCod();
    }
}
