package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table( name = "tax" )
public class TaxEntity extends AuditTableEntity implements Serializable {

    @Id
    public String TaxCod;
    public BigDecimal TaxRateValue;
    public String Name;
    public String Description;
}
