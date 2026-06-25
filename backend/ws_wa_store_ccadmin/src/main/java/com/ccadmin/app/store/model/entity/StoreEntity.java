package com.ccadmin.app.store.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "store")
public class StoreEntity extends AuditTableEntity implements Serializable {

    @Id
    public String StoreCod;
    public String Name;
    public String Description;
    public String Address;
    public String UbigeoCod;
    public String CompanyCod;
}
