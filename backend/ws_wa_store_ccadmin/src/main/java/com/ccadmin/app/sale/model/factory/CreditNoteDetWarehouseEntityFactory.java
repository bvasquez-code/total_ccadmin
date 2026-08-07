package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetWarehouseEntity;

public final class CreditNoteDetWarehouseEntityFactory {

    private CreditNoteDetWarehouseEntityFactory() {
    }

    public static CreditNoteDetWarehouseEntity fromReturnedDetail(
            CreditNoteDetEntity detail,
            String warehouseCod
    ) {
        CreditNoteDetWarehouseEntity warehouse = new CreditNoteDetWarehouseEntity();
        warehouse.CreditNoteCod = detail.CreditNoteCod;
        warehouse.ItemNumber = detail.ItemNumber;
        warehouse.ProductCod = detail.ProductCod;
        warehouse.Variant = detail.Variant;
        warehouse.WarehouseCod = warehouseCod;
        warehouse.NumUnit = detail.NumUnitStockReturned;
        warehouse.ProductUnitName = detail.ProductUnitName;
        warehouse.ProductUnitFactor = detail.ProductUnitFactor;
        warehouse.LotNumber = detail.LotNumber;
        warehouse.ExpirationDate = detail.ExpirationDate;
        return warehouse;
    }
}
