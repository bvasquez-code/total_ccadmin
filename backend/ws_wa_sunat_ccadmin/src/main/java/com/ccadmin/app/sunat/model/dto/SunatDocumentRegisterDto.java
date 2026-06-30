package com.ccadmin.app.sunat.model.dto;

import java.math.BigDecimal;
import java.util.Date;

public class SunatDocumentRegisterDto {
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String SunatDocumentType;
    public String Series;
    public int Correlative;
    public String IssuerRuc;
    public Date IssueDate;
    public String CurrencyCod;
    public BigDecimal NumTotalPrice;
    public BigDecimal NumTotalTax;
    public String OriginalSunatDocumentCod;
    public String RelatedDocumentNumber;
    public String RelatedDocumentType;
}
