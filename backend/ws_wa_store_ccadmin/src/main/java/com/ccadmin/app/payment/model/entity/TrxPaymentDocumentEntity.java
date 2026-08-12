package com.ccadmin.app.payment.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "trx_payments_document")
public class TrxPaymentDocumentEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long TrxPaymentDocumentId;
    public Long TrxPaymentId;
    public String DocumentType;
    public String ContentEncoding;
    @Lob
    @Column(columnDefinition = "longtext")
    public String Content;
    public String FileName;
    public String ContentType;
    public Long SizeBytes;
    public String Sha256Hash;
    public String SourceType;
    public Date PurgeAfterDate;
}
