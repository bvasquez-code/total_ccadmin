package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.delivery.model.idto.IUbigeoOptionDto;

public class UbigeoOptionDto {

    public String Code;
    public String Name;

    public UbigeoOptionDto() {
    }

    public UbigeoOptionDto(IUbigeoOptionDto source) {
        this.Code = source.getCode();
        this.Name = source.getName();
    }
}
