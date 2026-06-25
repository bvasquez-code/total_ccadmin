package com.ccadmin.app.security.model.entity;

import com.ccadmin.app.security.model.entity.id.ProfileMenuID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "profile_menu")
@IdClass(ProfileMenuID.class)
public class ProfileMenuEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProfileCod;
    @Id
    public String MenuCod;
}
