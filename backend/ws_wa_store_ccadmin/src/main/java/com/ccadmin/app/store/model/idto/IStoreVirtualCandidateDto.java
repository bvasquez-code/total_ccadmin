package com.ccadmin.app.store.model.idto;

import java.math.BigDecimal;

public interface IStoreVirtualCandidateDto {

    String getStoreCod();
    String getName();
    String getDescription();
    String getAddress();
    String getUbigeoCod();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    String getAllowsAutomaticDelivery();
    BigDecimal getAutomaticDeliveryRadiusKm();
    String getAllowsScheduledDelivery();
    BigDecimal getScheduledDeliveryMaxRadiusKm();
    String getAllowsStorePickup();
    Integer getPreparationTimeMinutes();
}
