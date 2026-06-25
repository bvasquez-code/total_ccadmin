package com.ccadmin.app.system.model.dto;

import java.util.ArrayList;
import java.util.List;

public class CounterfoilRegisterListDto {

    public List<CounterfoilRegisterDto> registerList;

    public CounterfoilRegisterListDto() {
        this.registerList = new ArrayList<>();
    }

    public CounterfoilRegisterListDto(List<CounterfoilRegisterDto> registerList) {
        this.registerList = registerList;
    }
}
