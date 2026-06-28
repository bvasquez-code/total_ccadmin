package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.constants.StockZoneConstants;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.model.entity.StockZoneMovementEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
import com.ccadmin.app.product.repository.StockZoneMovementRepository;
import com.ccadmin.app.product.shared.ProductFindCreateShared;
import com.ccadmin.app.product.shared.ProductInfoShared;
import com.ccadmin.app.product.shared.ProductInfoWarehouseShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockZoneMovementService {

    @Autowired
    private ProductInfoShared productInfoShared;
    @Autowired
    private ProductInfoWarehouseShared productInfoWarehouseShared;
    @Autowired
    private ProductFindCreateShared productFindCreateShared;
    @Autowired
    private StockZoneMovementRepository stockZoneMovementRepository;

    @Transactional
    public void addCreditNoteUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        for (var detail : detailList) {
            this.applyMovement(
                    detail,
                    creditNoteHead.StoreCod,
                    warehouse.WarehouseCod,
                    StockZoneConstants.ZONE_EXTERNAL,
                    StockZoneConstants.ZONE_UNAVAILABLE,
                    0,
                    detail.NumUnit,
                    0,
                    detail.NumUnit,
                    userCod
            );
        }
    }

    public boolean existsCreditNoteUnavailableStock(String creditNoteCod) {
        return this.stockZoneMovementRepository.countBySource(
                StockZoneConstants.SOURCE_CREDIT_NOTE,
                creditNoteCod,
                StockZoneConstants.ZONE_EXTERNAL,
                StockZoneConstants.ZONE_UNAVAILABLE
        ) > 0;
    }

    @Transactional
    public void resolveCreditNoteUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        for (var detail : detailList) {
            int returned = detail.NumUnitStockReturned == null ? 0 : detail.NumUnitStockReturned;
            if (returned < 0 || returned > detail.NumUnit) {
                throw new SaleException("Cantidad de retorno invalida para el producto " + detail.ProductCod);
            }

            int rejected = detail.NumUnit - returned;
            if (returned > 0) {
                this.applyMovement(
                        detail,
                        creditNoteHead.StoreCod,
                        warehouse.WarehouseCod,
                        StockZoneConstants.ZONE_UNAVAILABLE,
                        StockZoneConstants.ZONE_PHYSICAL,
                        returned,
                        returned * -1,
                        0,
                        0,
                        userCod
                );
            }
            if (rejected > 0) {
                this.applyMovement(
                        detail,
                        creditNoteHead.StoreCod,
                        warehouse.WarehouseCod,
                        StockZoneConstants.ZONE_UNAVAILABLE,
                        StockZoneConstants.ZONE_OUT,
                        0,
                        rejected * -1,
                        0,
                        rejected * -1,
                        userCod
                );
            }
        }
    }

    private void applyMovement(
            CreditNoteDetEntity detail,
            String storeCod,
            String warehouseCod,
            String sourceZone,
            String targetZone,
            int physicalDelta,
            int unavailableDelta,
            int reservedDelta,
            int totalDelta,
            String userCod
    ) throws SaleException {
        ProductInfoEntity productInfo = this.productInfoShared.findById(
                new ProductInfoId(detail.ProductCod, detail.Variant, storeCod)
        );
        ProductInfoWarehouseEntity productInfoWarehouse = this.productInfoWarehouseShared.findById(
                new ProductInfoWarehouseId(detail.ProductCod, detail.Variant, warehouseCod)
        );

        StockZoneMovementEntity movement = new StockZoneMovementEntity();
        movement.OperationCod = detail.CreditNoteCod;
        movement.ItemNumber = detail.ItemNumber;
        movement.SourceTable = StockZoneConstants.SOURCE_CREDIT_NOTE;
        movement.ProductCod = detail.ProductCod;
        movement.Variant = detail.Variant;
        movement.StoreCod = storeCod;
        movement.WarehouseCod = warehouseCod;
        movement.SourceZone = sourceZone;
        movement.TargetZone = targetZone;
        movement.NumStockMoved = Math.max(Math.abs(physicalDelta), Math.max(Math.abs(unavailableDelta), Math.abs(totalDelta)));
        movement.NumPhysicalStockBefore = productInfoWarehouse.NumPhysicalStock;
        movement.NumUnavailableStockBefore = productInfoWarehouse.NumUnavailableStock;
        movement.NumReservedStockBefore = productInfoWarehouse.NumReservedStock;
        movement.NumTotalStockBefore = productInfoWarehouse.NumTotalStock;
        movement.LotNumber = detail.LotNumber;
        movement.ExpirationDate = detail.ExpirationDate;

        try {
            productInfo.applyStockZoneMovement(physicalDelta, unavailableDelta, reservedDelta, totalDelta);
            productInfoWarehouse.applyStockZoneMovement(physicalDelta, unavailableDelta, reservedDelta, totalDelta);
        } catch (IllegalStateException ex) {
            throw new SaleException(ex.getMessage());
        }

        movement.NumPhysicalStockAfter = productInfoWarehouse.NumPhysicalStock;
        movement.NumUnavailableStockAfter = productInfoWarehouse.NumUnavailableStock;
        movement.NumReservedStockAfter = productInfoWarehouse.NumReservedStock;
        movement.NumTotalStockAfter = productInfoWarehouse.NumTotalStock;
        movement.addSession(userCod);

        productInfo.addSession(userCod, false);
        productInfoWarehouse.addSession(userCod, false);
        this.productInfoShared.save(productInfo);
        this.productInfoWarehouseShared.save(productInfoWarehouse);
        this.stockZoneMovementRepository.save(movement);
        this.productFindCreateShared.save(detail.ProductCod, storeCod);
    }
}
