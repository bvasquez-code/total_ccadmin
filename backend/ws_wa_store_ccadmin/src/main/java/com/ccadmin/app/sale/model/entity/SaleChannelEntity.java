package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "sale_channel")
public class SaleChannelEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SaleCod;
    public String ChannelCod;

    public SaleChannelEntity() {
    }
}
