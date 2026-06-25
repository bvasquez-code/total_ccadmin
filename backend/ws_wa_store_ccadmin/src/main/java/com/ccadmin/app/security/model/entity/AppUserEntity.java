package com.ccadmin.app.security.model.entity;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.user.model.entity.UserStoreEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table( name = "app_user")
public class AppUserEntity extends AuditTableEntity implements Serializable {

    @Id
    public String UserCod;
    public String PersonCod;
    public String Password;
    public String PasswordDecoded;
    public String Email;
    public String CreationCode;
    public Date DateExpire;
    public String RecoveryCod;

    @Transient
    public PersonEntity Person;

    @Transient
    public List<UserProfileEntity> UserProfileList;

    @Transient
    public List<UserStoreEntity> UserStoreList;

    public AppUserEntity()
    {
        this.Person = new PersonEntity();
        this.UserProfileList = new ArrayList<>();
        this.UserStoreList = new ArrayList<>();
    }

    public void clearDataSensitive()
    {
        this.PasswordDecoded = "";
        this.Password = "";
        this.CreationCode = "";
        this.RecoveryCod = "";
        this.DateExpire = null;
    }

}
