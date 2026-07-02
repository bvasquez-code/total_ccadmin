package com.ccadmin.app.sunat.model.dto;

import com.ccadmin.app.sunat.model.constants.SunatDocumentTypeConst;

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
    public String OperationTypeCode = "0101";
    public String PaymentCondition = "Contado";
    public List<SunatPaymentTermDto> PaymentTerms;
    public String Note;
    public SunatPartyDto Supplier;
    public SunatPartyDto Customer;
    public SunatDocumentTotalsDto Totals;
    public List<SunatDocumentLineDto> Lines;

    public SunatElectronicDocumentDto toElectronicDocumentDto() {
        SunatElectronicDocumentDto dto = new SunatElectronicDocumentDto();
        dto.SourceModule = SourceModule;
        dto.SourceDocumentCod = SourceDocumentCod;
        dto.SourceDocumentType = SourceDocumentType;
        dto.SunatDocumentType = SunatDocumentTypeConst.FACTURA;
        dto.Series = Series;
        dto.Correlative = Correlative;
        dto.IssueDate = IssueDate;
        dto.IssueTime = IssueTime;
        dto.CurrencyCod = CurrencyCod;
        dto.OperationTypeCode = OperationTypeCode;
        dto.PaymentCondition = PaymentCondition;
        dto.PaymentTerms = PaymentTerms;
        dto.Note = Note;
        dto.Supplier = Supplier;
        dto.Customer = Customer;
        dto.Totals = Totals;
        dto.Lines = Lines;
        return dto;
    }
}
