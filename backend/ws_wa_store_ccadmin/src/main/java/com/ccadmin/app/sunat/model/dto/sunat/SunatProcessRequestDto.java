package com.ccadmin.app.sunat.model.dto.sunat;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

public abstract class SunatProcessRequestDto {

    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String Series;
    public int Correlative;
    public Date IssueDate;
    public String IssueTime;

    @JsonIgnore
    public String StoreCod;

    @JsonIgnore
    public String AuditUserCod;
}
