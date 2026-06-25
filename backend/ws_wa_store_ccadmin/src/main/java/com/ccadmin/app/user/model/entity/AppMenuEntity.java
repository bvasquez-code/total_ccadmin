package com.ccadmin.app.user.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "app_menu")
public class AppMenuEntity extends AuditTableEntity implements Serializable {

    @Id
    public String MenuCod;
    public String Name;
    public String Description;
    public String IsMenuDad;
    public String MenuDadCod;

    public AppMenuEntity()
    {

    }
}
