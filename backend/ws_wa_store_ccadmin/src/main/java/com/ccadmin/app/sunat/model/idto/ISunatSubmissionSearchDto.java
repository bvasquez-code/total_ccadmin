package com.ccadmin.app.sunat.model.idto;

import java.util.Date;

public interface ISunatSubmissionSearchDto {
    String getSunatSubmissionCod();
    String getStoreCod();
    String getStoreName();
    String getSourceModule();
    String getSourceDocumentCod();
    String getSourceDocumentType();
    String getSunatDocumentType();
    String getSeries();
    Integer getCorrelative();
    String getRequestType();
    String getSendStatus();
    String getSunatStatus();
    String getRemoteSunatDocumentCod();
    String getSunatTicket();
    Integer getAttemptCount();
    Date getLastAttemptDate();
    Date getLastSuccessDate();
    String getLastAttemptUser();
    String getLastResponseStatus();
    String getLastErrorReason();
    String getCreationUser();
    Date getCreationDate();
    String getModifyUser();
    Date getModifyDate();
}
