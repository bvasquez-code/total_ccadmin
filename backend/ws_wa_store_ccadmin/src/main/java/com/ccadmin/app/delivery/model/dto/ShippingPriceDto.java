package com.ccadmin.app.delivery.model.dto;

import java.math.BigDecimal;

public class ShippingPriceDto {

    public String DeliveryTypeCod;
    public String ScheduleType;
    public String ShippingConfigCod;
    public String ProductCod;
    public String Description;
    public BigDecimal DistanceKm;
    public BigDecimal PriceBase;
    public BigDecimal PricePerKm;
    public BigDecimal Amount;
    public DeliveryCoverageDto Coverage;
    public ShippingScheduleDto Schedule;
}
