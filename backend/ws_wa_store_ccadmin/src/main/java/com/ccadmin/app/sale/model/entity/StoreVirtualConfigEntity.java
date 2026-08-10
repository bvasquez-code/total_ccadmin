package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "store_virtual_config")
public class StoreVirtualConfigEntity extends AuditTableEntity implements Serializable {

    @Id
    public String StoreCod;
    public String AllowsAutomaticDelivery = "N";
    public BigDecimal AutomaticDeliveryRadiusKm;
    public String AllowsScheduledDelivery = "N";
    public BigDecimal ScheduledDeliveryMaxRadiusKm;
    public String AllowsStorePickup = "N";
    public int PreparationTimeMinutes;

    public StoreVirtualConfigEntity() {
    }
}
