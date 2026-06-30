package com.ccadmin.app.sunat.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "sunat_document_attempt")
public class SunatDocumentAttemptEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long AttemptId;
    public String SunatDocumentCod;
    public String OperationType;
    public String Environment;
    public String Endpoint;
    public int AttemptNumber;
    public String Success;
    public String TechnicalMessage;
    public String FunctionalMessage;
    public String SunatTicket;
    public String SunatResponseCode;

    public SunatDocumentAttemptEntity validate() {
        if (SunatDocumentCod == null || SunatDocumentCod.isBlank())
            throw new IllegalArgumentException("SunatDocumentCod requerido");
        if (OperationType == null || OperationType.isBlank())
            throw new IllegalArgumentException("OperationType requerido");
        if (Success == null || Success.isBlank())
            Success = "N";
        return this;
    }

    @Override
    public SunatDocumentAttemptEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
