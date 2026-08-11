package com.ccadmin.app.client.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "client_account")
public class ClientAccountEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long ClientAccountID;
    public String ClientCod;
    public String Email;
    public String PasswordHash;
    public String IsEmailVerified = "N";
    public String EmailVerificationTokenHash;
    public Date EmailVerificationExpireDate;
    public String PasswordRecoveryTokenHash;
    public Date PasswordRecoveryExpireDate;
    public int FailedLoginAttempts;
    public Date LockUntilDate;
    public Date LastLoginDate;

    public ClientAccountEntity() {
    }
}
