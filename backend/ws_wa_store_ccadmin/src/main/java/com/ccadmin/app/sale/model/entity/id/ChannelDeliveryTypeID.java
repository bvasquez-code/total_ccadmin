package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;
import java.util.Objects;

public class ChannelDeliveryTypeID implements Serializable {

    public String ChannelCod;
    public String DeliveryTypeCod;

    public ChannelDeliveryTypeID() {
    }

    public ChannelDeliveryTypeID(String channelCod, String deliveryTypeCod) {
        ChannelCod = channelCod;
        DeliveryTypeCod = deliveryTypeCod;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof ChannelDeliveryTypeID that)) return false;
        return Objects.equals(ChannelCod, that.ChannelCod)
                && Objects.equals(DeliveryTypeCod, that.DeliveryTypeCod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChannelCod, DeliveryTypeCod);
    }
}
