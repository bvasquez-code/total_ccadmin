package com.ccadmin.app.delivery.model.dto;

import java.math.BigDecimal;

public class CheckoutDeliveryDto {

    public String DeliveryTypeCod;
    public String IsThirdParty = "N";
    public String Names;
    public String DocumentType;
    public String DocumentNumber;
    public String Phone;
    public String Email;
    public String Address;
    public String Reference;
    public String UbigeoCod;
    public BigDecimal Latitude;
    public BigDecimal Longitude;
    public BigDecimal EstimatedDistanceKm;
    public String Instructions;
    public String ScheduledFrom;
    public String ScheduledTo;
}
