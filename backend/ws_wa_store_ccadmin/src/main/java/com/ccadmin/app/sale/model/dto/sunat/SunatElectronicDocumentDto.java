package com.ccadmin.app.sale.model.dto.sunat;

import java.util.Date;
import java.util.List;

public class SunatElectronicDocumentDto {
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String SunatDocumentType;
    public String Series;
    public int Correlative;
    public Date IssueDate;
    public String IssueTime;
    public String CurrencyCod;
    public String OperationTypeCode = "0101";
    public String PaymentCondition = "Contado";
    public List<SunatPaymentTermDto> PaymentTerms;
    public String Note;
    public SunatPartyDto Supplier;
    public SunatPartyDto Customer;
    public SunatDocumentTotalsDto Totals;
    public SunatDiscrepancyResponseDto DiscrepancyResponse;
    public List<SunatRelatedDocumentDto> RelatedDocuments;
    public List<SunatDocumentLineDto> Lines;
}
