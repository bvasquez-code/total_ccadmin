package com.ccadmin.app.system.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "currency")
public class CurrencyEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CurrencyCod;
    public String CurrencyAbbr;
    public String CurrencySymbol;
    public String CurrencyName;
    public String CurrencyDesc;
    public String IsCurrencySystem;
    public BigDecimal NumExchangevalue;

    public CurrencyEntity validate() {
        if (CurrencyCod == null || CurrencyCod.isBlank()) {
            throw new IllegalArgumentException("CurrencyCod requerido");
        }
        if (NumExchangevalue != null && NumExchangevalue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("NumExchangevalue no debe ser negativo");
        }
        return this;
    }

    @Override
    public CurrencyEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
