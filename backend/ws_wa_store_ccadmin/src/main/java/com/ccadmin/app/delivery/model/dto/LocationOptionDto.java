package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.shared.model.idto.ILocationOptionDto;

public class LocationOptionDto {

    public String Code;
    public String Name;
    public Double Latitude;
    public Double Longitude;

    public LocationOptionDto() {
    }

    public LocationOptionDto(ILocationOptionDto source) {
        this.Code = source.getCode();
        this.Name = source.getName();
        this.Latitude = source.getLatitude();
        this.Longitude = source.getLongitude();
    }
}
