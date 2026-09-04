package com.ccadmin.app.sunat.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "sunat_submission")
public class SunatSubmissionEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SunatSubmissionCod;
    public String StoreCod;
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String SunatDocumentType;
    public String Series;
    public Integer Correlative;
    public String RequestType;
    public String EndpointKey;
    @Column(columnDefinition = "longtext")
    public String PayloadJson;
    public String SendStatus;
    public String SunatStatus;
    public String RemoteSunatDocumentCod;
    public String SunatTicket;
    public Integer AttemptCount;
    public Date LastAttemptDate;
    public Date LastSuccessDate;
    public String LastAttemptUser;
    public String LastResponseStatus;
    @Column(columnDefinition = "longtext")
    public String LastResponseJson;
    @Column(columnDefinition = "longtext")
    public String LastErrorReason;

    @Override
    public SunatSubmissionEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
