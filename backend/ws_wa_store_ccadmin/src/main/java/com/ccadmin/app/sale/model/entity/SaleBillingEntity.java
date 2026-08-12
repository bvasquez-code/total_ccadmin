package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;

@Entity
@Table(name = "sale_billing")
public class SaleBillingEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SaleCod;
    public String PersonCod;
    public String DocumentTypeRequest;
    public String DocumentType;
    public String DocumentNum;
    public String LegalName;
    public String CommercialName;
    public String Address;
    public String UbigeoCod;

    @Transient
    public PersonEntity Person;
}
