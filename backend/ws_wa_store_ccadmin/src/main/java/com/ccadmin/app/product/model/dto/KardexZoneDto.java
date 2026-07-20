package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;

public class KardexZoneDto {

    public KardexZoneEntity kardexZone;
    public ProductEntity product;

    public KardexZoneDto(KardexZoneEntity kardexZone, ProductEntity product) {
        this.kardexZone = kardexZone;
        this.product = product;
    }
}
