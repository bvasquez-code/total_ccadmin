package com.ccadmin.app.system.model.dto;

import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import com.ccadmin.app.system.model.entity.CounterfoilStoreEntity;

public class CounterfoilRegisterDto {

    public CounterfoilEntity counterfoil;
    public CounterfoilStoreEntity counterfoilStore;

    public CounterfoilRegisterDto() { }

    public CounterfoilRegisterDto(CounterfoilEntity counterfoil,CounterfoilStoreEntity counterfoilStore) {
        this.counterfoil = counterfoil;
        this.counterfoilStore = counterfoilStore;
    }
}
