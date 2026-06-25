package com.ccadmin.app.supplier.model.entity;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;

@Entity
@Table( name = "supplier" )
public class SupplierEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SupplierCod;
    public String PersonCod;

    @Transient
    public PersonEntity Person;

    public SupplierEntity()
    {

    }
}
