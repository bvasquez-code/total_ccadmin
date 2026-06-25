package com.ccadmin.app.system.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "app_file")
public class AppFileEntity extends AuditTableEntity implements Serializable {

    @Id
    public String FileCod;
    public String Name;
    public String Description;
    public String Route;
    public String FileType;
}
