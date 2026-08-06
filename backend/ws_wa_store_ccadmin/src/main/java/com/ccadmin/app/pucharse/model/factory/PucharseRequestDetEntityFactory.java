package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;

public final class PucharseRequestDetEntityFactory {

    private PucharseRequestDetEntityFactory() {
    }

    public static PucharseRequestDetEntity fromSaveRequest(
            PucharseRequestDetEntity source,
            PucharseRequestDetEntity current
    ) {
        PucharseRequestDetEntity detail = current == null ? source : current;
        detail.PucharseReqCod = source.PucharseReqCod;
        detail.ProductCod = source.ProductCod;
        detail.Variant = source.Variant;
        detail.NumUnit = source.NumUnit;
        detail.NumUnitPrice = source.NumUnitPrice;
        detail.NumTotalPrice = source.NumTotalPrice;
        detail.ProductUnitName = source.ProductUnitName;
        detail.ProductUnitFactor = source.ProductUnitFactor;
        detail.Status = "A";
        return detail;
    }
}
