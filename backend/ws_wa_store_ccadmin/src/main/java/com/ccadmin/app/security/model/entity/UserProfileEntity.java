package com.ccadmin.app.security.model.entity;

import com.ccadmin.app.security.model.entity.id.UserProfileID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table( name = "user_profile")
@IdClass(UserProfileID.class)
public class UserProfileEntity extends AuditTableEntity {

    @Id
    public String UserCod;
    @Id
    public String ProfileCod;

    public UserProfileEntity()
    {

    }
}
