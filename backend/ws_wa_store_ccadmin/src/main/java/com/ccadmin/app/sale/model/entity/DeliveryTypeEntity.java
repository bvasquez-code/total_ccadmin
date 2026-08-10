package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "delivery_type")
public class DeliveryTypeEntity extends AuditTableEntity implements Serializable {

    @Id
    public String DeliveryTypeCod;
    public String Name;
    public String Description;

    public DeliveryTypeEntity() {
    }
}
