package com.ccadmin.app.delivery.model.dto;

import java.math.BigDecimal;

public class StoreLocationRequestDto {

    public BigDecimal Latitude;
    public BigDecimal Longitude;
    public String UbigeoCod;
    public String Address;
    public String IsManual = "N";
}
