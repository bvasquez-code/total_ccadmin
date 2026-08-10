package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "client_address")
public class ClientAddressEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long ClientAddressID;
    public String ClientCod;
    public String Alias;
    public String Names;
    public String Phone;
    public String Address;
    public String Reference;
    public String UbigeoCod;
    public BigDecimal Latitude;
    public BigDecimal Longitude;
    public String Instructions;
    public String IsDefault = "N";

    public ClientAddressEntity() {
    }
}
