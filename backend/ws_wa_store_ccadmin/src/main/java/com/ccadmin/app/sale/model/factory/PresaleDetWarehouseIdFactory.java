package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.id.PresaleDetWarehouseID;

public final class PresaleDetWarehouseIdFactory {

    private PresaleDetWarehouseIdFactory() {
    }

    public static PresaleDetWarehouseID fromDetail(PresaleDetEntity detail) {
        PresaleDetWarehouseID id = new PresaleDetWarehouseID();
        id.PresaleCod = detail.PresaleCod;
        id.ItemNumber = detail.ItemNumber;
        return id;
    }
}
