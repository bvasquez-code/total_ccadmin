package com.ccadmin.app.cash.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "cash_register")
public class CashRegisterEntity extends AuditTableEntity implements Serializable {

    @Id
    public String RegisterCod;   // VARCHAR(8)
    public String StoreCod;      // VARCHAR(4)
    public String Name;          // VARCHAR(32)
    public String Description;   // VARCHAR(128)
    public String SerialNumber;  // VARCHAR(64)

    public CashRegisterEntity validate() {
        if (RegisterCod == null || RegisterCod.isBlank())
            throw new IllegalArgumentException("RegisterCod requerido");
        if (StoreCod == null || StoreCod.isBlank())
            throw new IllegalArgumentException("StoreCod requerido");
        if (Name == null || Name.isBlank())
            throw new IllegalArgumentException("Name requerido");
        return this;
    }

    @Override
    public CashRegisterEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}

