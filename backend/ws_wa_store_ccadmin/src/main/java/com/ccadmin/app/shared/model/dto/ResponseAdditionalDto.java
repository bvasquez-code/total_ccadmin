package com.ccadmin.app.shared.model.dto;

public class ResponseAdditionalDto {

    public String Name;
    public Object Data;

    public ResponseAdditionalDto()
    {

    }

    public ResponseAdditionalDto(String name, Object data) {
        Name = name;
        Data = data;
    }
}
