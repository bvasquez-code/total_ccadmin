package com.ccadmin.app.sunat.model.dto.sunat;

import java.util.List;

public class SunatReceiptProcessRequestDto extends SunatProcessRequestDto {
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
