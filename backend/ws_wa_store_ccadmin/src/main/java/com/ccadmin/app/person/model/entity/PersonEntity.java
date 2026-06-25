package com.ccadmin.app.person.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "person")
public class PersonEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PersonCod;
    public String PersonType;
    public String DocumentType;
    public String DocumentNum;
    public String Names;
    public String LastNames;
    public String CommercialName;
    public String BusinessName;
    public String Address;
    public String UbigeoCod;
    public String Phone;
    public String CellPhone;
    public String Email;


}
