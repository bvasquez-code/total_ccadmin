package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IDocumentsReportDto;
import java.math.BigDecimal;
import java.util.Date;

public class DocumentsReportDto {
    public String DocumentCod;
    public String CounterfoilCod;
    public String DocumentType;
    public String DocumentRole;
    public String OperationCod;
    public Date ReportDate;
    public String StoreName;
    public String ClientCod;
    public String CurrencyCod;
    public String DocumentStatus;
    public BigDecimal Amount;
    public String StoreCod;

    public DocumentsReportDto(IDocumentsReportDto row) {
        this.DocumentCod = row.getDocumentCod();
        this.CounterfoilCod = row.getCounterfoilCod();
        this.DocumentType = row.getDocumentType();
        this.DocumentRole = row.getDocumentRole();
        this.OperationCod = row.getOperationCod();
        this.ReportDate = row.getReportDate();
        this.StoreName = row.getStoreName();
        this.ClientCod = row.getClientCod();
        this.CurrencyCod = row.getCurrencyCod();
        this.DocumentStatus = row.getDocumentStatus();
        this.Amount = row.getAmount();
        this.StoreCod = row.getStoreCod();
    }
}
