package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.sale.model.entity.id.SaleDocumentID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table( name = "sale_document" )
@IdClass(SaleDocumentID.class)
public class SaleDocumentEntity extends AuditTableEntity implements Serializable {

    @Id
    public String DocumentCod;
    @Id
    public String SaleCod;
    public String CounterfoilCod;
    public String DocumentType;
    public String DocumentRole;
    public String ClientCod;
    public Date IssueDate;

    @Transient
    public ClientEntity Client;

}
