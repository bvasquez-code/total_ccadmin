package com.ccadmin.app.delivery.model.dto;

import java.math.BigDecimal;

public class IpGeolocationDto {

    public final BigDecimal Latitude;
    public final BigDecimal Longitude;
    public final String Address;

    public IpGeolocationDto(BigDecimal Latitude, BigDecimal Longitude, String Address) {
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.Address = Address;
    }
}
