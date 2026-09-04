package com.ccadmin.app.sunat.model.dto;

import com.ccadmin.app.sunat.model.idto.ISunatSubmissionSearchDto;

import java.util.Date;

public class SunatSubmissionResultDto {

    public String SunatSubmissionCod;
    public String StoreCod;
    public String StoreName;
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String SunatDocumentType;
    public String Series;
    public Integer Correlative;
    public String RequestType;
    public String SendStatus;
    public String SunatStatus;
    public String RemoteSunatDocumentCod;
    public String SunatTicket;
    public Integer AttemptCount;
    public Date LastAttemptDate;
    public Date LastSuccessDate;
    public String LastAttemptUser;
    public String LastResponseStatus;
    public String LastErrorReason;
    public String CreationUser;
    public Date CreationDate;
    public String ModifyUser;
    public Date ModifyDate;

    public SunatSubmissionResultDto(ISunatSubmissionSearchDto submission) {
        this.SunatSubmissionCod = submission.getSunatSubmissionCod();
        this.StoreCod = submission.getStoreCod();
        this.StoreName = submission.getStoreName();
        this.SourceModule = submission.getSourceModule();
        this.SourceDocumentCod = submission.getSourceDocumentCod();
        this.SourceDocumentType = submission.getSourceDocumentType();
        this.SunatDocumentType = submission.getSunatDocumentType();
        this.Series = submission.getSeries();
        this.Correlative = submission.getCorrelative();
        this.RequestType = submission.getRequestType();
        this.SendStatus = submission.getSendStatus();
        this.SunatStatus = submission.getSunatStatus();
        this.RemoteSunatDocumentCod = submission.getRemoteSunatDocumentCod();
        this.SunatTicket = submission.getSunatTicket();
        this.AttemptCount = submission.getAttemptCount();
        this.LastAttemptDate = submission.getLastAttemptDate();
        this.LastSuccessDate = submission.getLastSuccessDate();
        this.LastAttemptUser = submission.getLastAttemptUser();
        this.LastResponseStatus = submission.getLastResponseStatus();
        this.LastErrorReason = submission.getLastErrorReason();
        this.CreationUser = submission.getCreationUser();
        this.CreationDate = submission.getCreationDate();
        this.ModifyUser = submission.getModifyUser();
        this.ModifyDate = submission.getModifyDate();
    }
}
