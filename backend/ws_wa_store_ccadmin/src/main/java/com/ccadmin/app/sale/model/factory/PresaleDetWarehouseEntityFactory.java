package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;

public final class PresaleDetWarehouseEntityFactory {

    private PresaleDetWarehouseEntityFactory() {
    }

    public static PresaleDetWarehouseEntity fromDetail(
            PresaleDetEntity detail,
            WarehouseEntity defaultWarehouse,
            PresaleDetWarehouseEntity current
    ) {
        PresaleDetWarehouseEntity warehouse = current == null
                ? new PresaleDetWarehouseEntity()
                : current;
        warehouse.PresaleCod = detail.PresaleCod;
        warehouse.ItemNumber = detail.ItemNumber;
        warehouse.ProductCod = detail.ProductCod;
        warehouse.Variant = detail.Variant;
        warehouse.NumUnit = detail.NumUnit;
        warehouse.ProductUnitName = detail.ProductUnitName;
        warehouse.ProductUnitFactor = detail.ProductUnitFactor;
        warehouse.WarehouseCod = defaultWarehouse.WarehouseCod;
        warehouse.LotNumber = detail.LotNumber;
        warehouse.ExpirationDate = detail.ExpirationDate;
        return warehouse;
    }
}
