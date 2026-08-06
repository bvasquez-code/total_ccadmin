package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;
import com.ccadmin.app.pucharse.model.entity.id.PucharseRequestDetId;

public final class PucharseRequestDetIdFactory {

    private PucharseRequestDetIdFactory() {
    }

    public static PucharseRequestDetId fromEntity(
            PucharseRequestDetEntity detail
    ) {
        PucharseRequestDetId id = new PucharseRequestDetId();
        id.PucharseReqCod = detail.PucharseReqCod;
        id.ProductCod = detail.ProductCod;
        id.Variant = detail.Variant == null || detail.Variant.trim().isEmpty()
                ? "0000"
                : detail.Variant;
        return id;
    }
}
