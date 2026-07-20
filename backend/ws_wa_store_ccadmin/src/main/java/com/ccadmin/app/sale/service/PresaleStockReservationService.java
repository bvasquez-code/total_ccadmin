package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.sale.exception.PresaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleDetWarehouseRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PresaleStockReservationService {

    @Autowired
    private PresaleDetWarehouseRepository presaleDetWarehouseRepository;
    @Autowired
    private KardexZoneShared kardexZoneShared;

    @Transactional
    public void reserve(
            PresaleHeadEntity presaleHead,
            SaleHeadEntity saleHead,
            String userCod
    ) throws PresaleException {
        this.validateReservationState(presaleHead, saleHead);

        List<PresaleDetWarehouseEntity> detailList =
                this.presaleDetWarehouseRepository.findActiveByPresaleCod(presaleHead.PresaleCod);
        if (detailList.isEmpty()) {
            throw new PresaleException("La preventa no tiene stock asignado por almacen");
        }

        List<KardexZoneEntity> kardexZoneList = new ArrayList<>();
        Map<String, ProductInfoWarehouseEntity> stockCursorByWarehouse = new HashMap<>();
        for (PresaleDetWarehouseEntity detail : detailList) {
            if (detail.NumUnit <= 0) {
                throw new PresaleException("La cantidad a reservar debe ser mayor que cero");
            }
            if (this.kardexZoneShared.isApplied(
                    SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                    presaleHead.PresaleCod, detail.ItemNumber,
                    SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
            )) {
                continue;
            }
            String key = detail.ProductCod + "|" + detail.Variant + "|"
                    + presaleHead.StoreCod + "|" + detail.WarehouseCod;
            ProductInfoWarehouseEntity stockCursor = stockCursorByWarehouse.computeIfAbsent(
                    key,
                    ignored -> this.kardexZoneShared.findStockForUpdate(
                            detail.ProductCod, detail.Variant,
                            presaleHead.StoreCod, detail.WarehouseCod
                    )
            );
            kardexZoneList.addAll(KardexZoneEntity.buildPresaleReservation(
                    presaleHead, detail, stockCursor, userCod
            ));
        }
        this.kardexZoneShared.saveAll(kardexZoneList);
    }

    private void validateReservationState(
            PresaleHeadEntity presaleHead,
            SaleHeadEntity saleHead
    ) throws PresaleException {
        if (presaleHead == null || !StatusConst.CONFIRMED.equals(presaleHead.SaleStatus)) {
            throw new PresaleException("La preventa debe estar confirmada para reservar stock");
        }
        if (saleHead == null || !StatusConst.PENDING.equals(saleHead.SaleStatus)) {
            throw new PresaleException("La venta pendiente debe existir para reservar stock");
        }
        if (!presaleHead.PresaleCod.equals(saleHead.PresaleCod)) {
            throw new PresaleException("La venta pendiente no corresponde a la preventa confirmada");
        }
    }
}
