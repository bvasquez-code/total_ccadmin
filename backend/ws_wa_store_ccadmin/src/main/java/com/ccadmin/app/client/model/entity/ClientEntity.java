package com.ccadmin.app.client.model.entity;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.Optional;

@Entity
@Table( name = "client" )
public class ClientEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ClientCod;
    public String PersonCod;

    @Transient
    public PersonEntity Person;

    public ClientEntity()
    {

    }

}
