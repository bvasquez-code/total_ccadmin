package com.ccadmin.app.sunat.model.dto.sunat;

import java.util.Date;
import java.util.List;

public class SunatInvoiceProcessRequestDto {
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String Series;
    public int Correlative;
    public Date IssueDate;
    public String IssueTime;
    public String CurrencyCod;
    public String OperationTypeCode;
    public String PaymentCondition;
    public List<SunatPaymentTermDto> PaymentTerms;
    public String Note;
    public SunatPartyDto Supplier;
    public SunatPartyDto Customer;
    public SunatDocumentTotalsDto Totals;
    public List<SunatDocumentLineDto> Lines;
}
