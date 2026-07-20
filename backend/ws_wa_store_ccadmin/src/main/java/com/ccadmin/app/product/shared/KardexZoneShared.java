package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.service.KardexZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class KardexZoneShared {

    @Autowired
    private KardexZoneService kardexZoneService;

    public ProductInfoWarehouseEntity findStockForUpdate(
            String productCod, String variant, String storeCod, String warehouseCod
    ) {
        return this.kardexZoneService.findStockForUpdate(
                productCod, variant, storeCod, warehouseCod
        );
    }

    public List<KardexZoneEntity> saveAll(List<KardexZoneEntity> movementList) {
        return this.kardexZoneService.saveAll(movementList);
    }

    public List<KardexZoneEntity> findByEvent(
            String sourceTable, String operationCod, int itemNumber, String movementEvent
    ) {
        return this.kardexZoneService.findByEvent(
                sourceTable, operationCod, itemNumber, movementEvent
        );
    }

    public boolean isApplied(
            String sourceTable, String operationCod, int itemNumber, String movementEvent
    ) {
        return this.kardexZoneService.isApplied(
                sourceTable, operationCod, itemNumber, movementEvent
        );
    }

    public boolean hasLegacyUnavailableBaseline(
            String productCod, String variant, String storeCod, String warehouseCod,
            int requiredStock, Date operationCreationDate
    ) {
        return this.kardexZoneService.hasLegacyUnavailableBaseline(
                productCod, variant, storeCod, warehouseCod,
                requiredStock, operationCreationDate
        );
    }
}
