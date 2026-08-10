package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "presale_channel")
public class PresaleChannelEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PresaleCod;
    public String ChannelCod;

    public PresaleChannelEntity() {
    }
}
