package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "commercial_channel")
public class CommercialChannelEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ChannelCod;
    public String Name;
    public String Description;
    public String IsPublic = "N";

    public CommercialChannelEntity() {
    }
}
