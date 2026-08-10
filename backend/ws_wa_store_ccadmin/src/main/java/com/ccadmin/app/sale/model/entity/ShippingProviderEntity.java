package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "shipping_provider")
public class ShippingProviderEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ShippingProviderCod;
    public String ProviderType;
    public String Name;
    public String BusinessName;
    public String DocumentNumber;
    public String Phone;
    public String Email;
    public String TrackingUrl;
    public String Description;

    public ShippingProviderEntity() {
    }
}
