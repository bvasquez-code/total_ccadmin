package com.ccadmin.app.system.model.entity.id;

import java.io.Serializable;

public class CounterfoilStoreID implements Serializable {

    public String CounterfoilCod;
    public String StoreCod;

    public CounterfoilStoreID(String counterfoilCod, String storeCod) {
        CounterfoilCod = counterfoilCod;
        StoreCod = storeCod;
    }

    public CounterfoilStoreID() {
    }

}
