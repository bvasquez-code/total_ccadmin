package com.ccadmin.app.security.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_profile")
public class AppProfileEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProfileCod;
    public String Name;
    public String Description;

    @Transient
    public List<ProfileMenuEntity> permissionsList;

    public AppProfileEntity()
    {
        this.permissionsList = new ArrayList<>();
    }
}
