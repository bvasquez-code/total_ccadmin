package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.store.model.entity.StoreEntity;

import java.math.BigDecimal;

public class StoreDeliveryContextDto {

    public StoreEntity Store;
    public BigDecimal Latitude;
    public BigDecimal Longitude;
    public String Address;
    public BigDecimal DistanceKm;
    public String AllowsAutomaticDelivery;
    public String AllowsScheduledDelivery;
    public String AllowsStorePickup;
    public String DeliveryMessage;
}
