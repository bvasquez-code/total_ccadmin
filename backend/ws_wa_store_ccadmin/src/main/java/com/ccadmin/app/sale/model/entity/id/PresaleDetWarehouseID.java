package com.ccadmin.app.sale.model.entity.id;

import java.io.Serializable;

public class PresaleDetWarehouseID implements Serializable {

    public String PresaleCod;
    public int ItemNumber;

    public PresaleDetWarehouseID()
    {

    }

    public PresaleDetWarehouseID(String presaleCod, int itemNumber) {
        PresaleCod = presaleCod;
        ItemNumber = itemNumber;
    }
}
