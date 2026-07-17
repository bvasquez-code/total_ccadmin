package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
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
public class CreditNoteStockService {

    @Autowired
    private ProductInfoShared productInfoShared;
    @Autowired
    private ProductInfoWarehouseShared productInfoWarehouseShared;
    @Autowired
    private ProductFindCreateShared productFindCreateShared;

    @Transactional
    public void addUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        for (var detail : detailList) {
            this.applyAdjustment(
                    detail,
                    creditNoteHead.StoreCod,
                    warehouse.WarehouseCod,
                    0,
                    detail.NumUnit,
                    0,
                    detail.NumUnit,
                    userCod
            );
        }
    }

    @Transactional
    public void resolveUnavailableStock(
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
                this.applyAdjustment(
                        detail,
                        creditNoteHead.StoreCod,
                        warehouse.WarehouseCod,
                        returned,
                        returned * -1,
                        0,
                        0,
                        userCod
                );
            }
            if (rejected > 0) {
                this.applyAdjustment(
                        detail,
                        creditNoteHead.StoreCod,
                        warehouse.WarehouseCod,
                        0,
                        rejected * -1,
                        0,
                        rejected * -1,
                        userCod
                );
            }
        }
    }

    private void applyAdjustment(
            CreditNoteDetEntity detail,
            String storeCod,
            String warehouseCod,
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

        try {
            productInfo.applyStockAdjustment(physicalDelta, unavailableDelta, reservedDelta, totalDelta);
            productInfoWarehouse.applyStockAdjustment(physicalDelta, unavailableDelta, reservedDelta, totalDelta);
        } catch (IllegalStateException ex) {
            throw new SaleException(ex.getMessage());
        }

        productInfo.addSession(userCod, false);
        productInfoWarehouse.addSession(userCod, false);
        this.productInfoShared.save(productInfo);
        this.productInfoWarehouseShared.save(productInfoWarehouse);
        this.productFindCreateShared.save(detail.ProductCod, storeCod);
    }
}
