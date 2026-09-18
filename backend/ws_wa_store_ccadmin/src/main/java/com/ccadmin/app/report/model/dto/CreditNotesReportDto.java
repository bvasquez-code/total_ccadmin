package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.ICreditNotesReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class CreditNotesReportDto {
    public String CreditNoteCod;
    public String SaleCod;
    public Date ReportDate;
    public String StoreName;
    public String ClientName;
    public String CreditNoteStatus;
    public String Reason;
    public String CurrencyCod;
    public BigDecimal AmountNoTax;
    public BigDecimal Tax;
    public BigDecimal Amount;
    public Long Quantity;
    public Long ReturnedQuantity;
    public String IsStockReturned;
    public String IsPaid;
    public String StoreCod;

    public CreditNotesReportDto(ICreditNotesReportDto row) {
        this.CreditNoteCod = row.getCreditNoteCod();
        this.SaleCod = row.getSaleCod();
        this.ReportDate = row.getReportDate();
        this.StoreName = row.getStoreName();
        this.ClientName = row.getClientName();
        this.CreditNoteStatus = row.getCreditNoteStatus();
        this.Reason = row.getReason();
        this.CurrencyCod = row.getCurrencyCod();
        this.AmountNoTax = row.getAmountNoTax();
        this.Tax = row.getTax();
        this.Amount = row.getAmount();
        this.Quantity = row.getQuantity();
        this.ReturnedQuantity = row.getReturnedQuantity();
        this.IsStockReturned = row.getIsStockReturned();
        this.IsPaid = row.getIsPaid();
        this.StoreCod = row.getStoreCod();
    }
}
