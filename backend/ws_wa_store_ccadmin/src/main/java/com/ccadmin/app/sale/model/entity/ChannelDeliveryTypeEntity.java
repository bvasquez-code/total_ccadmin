package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.model.entity.id.ChannelDeliveryTypeID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "channel_delivery_type")
@IdClass(ChannelDeliveryTypeID.class)
public class ChannelDeliveryTypeEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ChannelCod;
    @Id
    public String DeliveryTypeCod;
    public String IsDefault = "N";

    public ChannelDeliveryTypeEntity() {
    }
}
