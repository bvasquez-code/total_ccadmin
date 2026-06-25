package com.ccadmin.app.system.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "system_document")
public class SystemDocumentEntity extends AuditTableEntity implements Serializable {

    @Id
    public String DocumentCod;
    public String DocumentType;
    public String ReferenceCod;
    public String Content;
}
