package com.ccadmin.app.sunat.model.dto;

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
    public String Note;
    public SunatPartyDto Supplier;
    public SunatPartyDto Customer;
    public SunatDocumentTotalsDto Totals;
    public SunatDiscrepancyResponseDto DiscrepancyResponse;
    public List<SunatRelatedDocumentDto> RelatedDocuments;
    public List<SunatDocumentLineDto> Lines;

    public SunatDocumentRegisterDto toRegisterDto() {
        SunatDocumentRegisterDto dto = new SunatDocumentRegisterDto();
        dto.SourceModule = SourceModule;
        dto.SourceDocumentCod = SourceDocumentCod;
        dto.SourceDocumentType = SourceDocumentType;
        dto.SunatDocumentType = SunatDocumentType;
        dto.Series = Series;
        dto.Correlative = Correlative;
        dto.IssuerRuc = Supplier == null ? null : Supplier.DocumentNumber;
        dto.IssueDate = IssueDate;
        dto.CurrencyCod = CurrencyCod;
        dto.NumTotalPrice = Totals == null ? null : Totals.PayableAmount;
        dto.NumTotalTax = Totals == null ? null : Totals.TaxAmount;
        if (RelatedDocuments != null && !RelatedDocuments.isEmpty()) {
            dto.RelatedDocumentNumber = RelatedDocuments.get(0).DocumentNumber;
            dto.RelatedDocumentType = RelatedDocuments.get(0).DocumentType;
        }
        return dto;
    }
}
