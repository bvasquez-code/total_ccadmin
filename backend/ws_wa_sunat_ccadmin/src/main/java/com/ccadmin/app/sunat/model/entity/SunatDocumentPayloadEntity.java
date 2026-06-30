package com.ccadmin.app.sunat.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "sunat_document_payload")
public class SunatDocumentPayloadEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SunatDocumentCod;
    @Column(columnDefinition = "longtext")
    public String PayloadJson;
    @Column(columnDefinition = "longtext")
    public String UnsignedXml;
    public String UnsignedXmlFileName;
    public Date XmlGeneratedDate;

    public SunatDocumentPayloadEntity validate() {
        if (SunatDocumentCod == null || SunatDocumentCod.isBlank())
            throw new IllegalArgumentException("SunatDocumentCod requerido");
        if (PayloadJson == null || PayloadJson.isBlank())
            throw new IllegalArgumentException("PayloadJson requerido");
        if (UnsignedXml == null || UnsignedXml.isBlank())
            throw new IllegalArgumentException("UnsignedXml requerido");
        return this;
    }

    @Override
    public SunatDocumentPayloadEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
