package com.ccadmin.app.system.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.system.model.entity.id.CounterfoilStoreID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "counterfoil_store")
@IdClass(CounterfoilStoreID.class)
public class CounterfoilStoreEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CounterfoilCod;  // char(6)
    @Id
    public String StoreCod;        // varchar(4)

    public CounterfoilStoreEntity validate() {
        if (CounterfoilCod == null || CounterfoilCod.isBlank())
            throw new IllegalArgumentException("CounterfoilCod requerido");
        if (StoreCod == null || StoreCod.isBlank())
            throw new IllegalArgumentException("StoreCod requerido");
        return this;
    }

    @Override
    public CounterfoilStoreEntity session(String userCod) { this.addSession(userCod); return this; }
}
