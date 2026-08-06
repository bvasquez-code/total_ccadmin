package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;

import java.math.BigDecimal;

public final class PucharseDetEntityFactory {

    private PucharseDetEntityFactory() {
    }

    public static PucharseDetEntity fromRequest(
            PucharseRequestDetEntity request,
            String pucharseCod,
            int itemNumber
    ) {
        PucharseDetEntity detail = new PucharseDetEntity();
        detail.PucharseCod = pucharseCod;
        detail.ItemNumber = itemNumber;
        detail.ProductCod = request.ProductCod;
        detail.Variant = request.Variant;
        detail.NumUnit = request.NumUnit;
        detail.NumUnitPrice = request.NumUnitPrice;
        detail.NumTotalPrice = request.NumTotalPrice;
        detail.ProductUnitName = request.ProductUnitName;
        detail.ProductUnitFactor = request.ProductUnitFactor;
        detail.IsKardexAffected = null;
        return detail;
    }

    public static PucharseDetEntity fromLotDetail(
            PucharseDetEntity originDetail,
            PucharseDetEntity lotDetail,
            int itemNumber,
            boolean isOriginLine
    ) {
        PucharseDetEntity detail = isOriginLine
                ? originDetail
                : new PucharseDetEntity();
        int numUnit = lotDetail.NumUnitDelivered > 0
                ? lotDetail.NumUnitDelivered
                : lotDetail.NumUnit;

        detail.PucharseCod = originDetail.PucharseCod;
        detail.ItemNumber = itemNumber;
        detail.ProductCod = originDetail.ProductCod;
        detail.Variant = originDetail.Variant;
        detail.NumUnit = numUnit;
        detail.NumUnitDelivered = numUnit;
        detail.NumUnitPrice = originDetail.NumUnitPrice;
        detail.NumTotalPrice = originDetail.NumUnitPrice == null
                ? BigDecimal.ZERO
                : originDetail.NumUnitPrice.multiply(BigDecimal.valueOf(numUnit));
        detail.ProductUnitName = originDetail.ProductUnitName;
        detail.ProductUnitFactor = originDetail.ProductUnitFactor;
        detail.IsKardexAffected = "S";
        detail.LotNumber = lotDetail.LotNumber;
        detail.ExpirationDate = lotDetail.ExpirationDate;
        detail.Status = "A";
        return detail;
    }
}
