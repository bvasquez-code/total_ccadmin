package com.ccadmin.app.store.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "warehouse")
public class WarehouseEntity extends AuditTableEntity implements Serializable {

    @Id
    public String WarehouseCod;
    public String StoreCod;
    public String WarehouseName;
    public String WarehouseDesc;
}
