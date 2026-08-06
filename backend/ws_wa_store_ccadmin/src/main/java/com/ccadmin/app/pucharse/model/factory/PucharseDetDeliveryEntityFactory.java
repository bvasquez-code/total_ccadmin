package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;

public final class PucharseDetDeliveryEntityFactory {

    private PucharseDetDeliveryEntityFactory() {
    }

    public static PucharseDetDeliveryEntity fromReceipt(
            PucharseDetEntity detail,
            String pucharseCod,
            String warehouseCod,
            int numUnit
    ) {
        PucharseDetDeliveryEntity delivery = new PucharseDetDeliveryEntity();
        delivery.PucharseCod = pucharseCod;
        delivery.ItemNumber = detail.ItemNumber;
        delivery.ProductCod = detail.ProductCod;
        delivery.Variant = detail.Variant;
        delivery.WarehouseCod = warehouseCod;
        delivery.NumUnit = numUnit;
        delivery.ProductUnitName = detail.ProductUnitName;
        delivery.ProductUnitFactor = detail.ProductUnitFactor;
        delivery.LotNumber = detail.LotNumber;
        delivery.ExpirationDate = detail.ExpirationDate;
        return delivery;
    }

    public static PucharseDetDeliveryEntity fromFullReceipt(
            PucharseDetEntity detail,
            String pucharseCod,
            String warehouseCod
    ) {
        return fromReceipt(
                detail, pucharseCod, warehouseCod, detail.NumUnit
        );
    }

    public static PucharseDetDeliveryEntity fromLotDetail(
            PucharseDetEntity detail,
            String warehouseCod
    ) {
        return fromReceipt(
                detail,
                detail.PucharseCod,
                warehouseCod,
                detail.NumUnitDelivered
        );
    }
}
