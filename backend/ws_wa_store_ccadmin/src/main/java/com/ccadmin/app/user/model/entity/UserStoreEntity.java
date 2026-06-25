package com.ccadmin.app.user.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.user.model.entity.id.UserStoreId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "user_store")
@IdClass(UserStoreId.class)
public class UserStoreEntity extends AuditTableEntity implements Serializable {

    @Id
    public String UserCod;
    @Id
    public String StoreCod;
    public String IsMainStore;

}
