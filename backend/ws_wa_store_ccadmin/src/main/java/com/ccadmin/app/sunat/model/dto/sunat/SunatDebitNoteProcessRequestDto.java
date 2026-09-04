package com.ccadmin.app.sunat.model.dto.sunat;

import java.util.List;

public class SunatDebitNoteProcessRequestDto extends SunatProcessRequestDto {
    public String CurrencyCod;
    public String Note;
    public SunatPartyDto Supplier;
    public SunatPartyDto Customer;
    public SunatDocumentTotalsDto Totals;
    public SunatDiscrepancyResponseDto DiscrepancyResponse;
    public List<SunatRelatedDocumentDto> RelatedDocuments;
    public List<SunatDocumentLineDto> Lines;
}
