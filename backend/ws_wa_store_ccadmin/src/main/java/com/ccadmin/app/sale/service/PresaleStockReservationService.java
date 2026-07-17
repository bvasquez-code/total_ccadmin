package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
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

import java.util.List;

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

        for (PresaleDetWarehouseEntity detail : detailList) {
            if (detail.NumUnit <= 0) {
                throw new PresaleException("La cantidad a reservar debe ser mayor que cero");
            }
            this.kardexZoneShared.apply(this.buildReservation(presaleHead, detail), userCod);
        }
    }

    private KardexZoneOperationDto buildReservation(
            PresaleHeadEntity presaleHead,
            PresaleDetWarehouseEntity detail
    ) {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = presaleHead.PresaleCod;
        operation.ItemNumber = detail.ItemNumber;
        operation.SourceTable = SaleConstants.KARDEX_ZONE_SOURCE_PRESALE;
        operation.MovementEvent = SaleConstants.KARDEX_ZONE_EVENT_RESERVATION;
        operation.ProductCod = detail.ProductCod;
        operation.Variant = detail.Variant;
        operation.StoreCod = presaleHead.StoreCod;
        operation.WarehouseCod = detail.WarehouseCod;
        operation.LotNumber = detail.LotNumber;
        operation.ExpirationDate = detail.ExpirationDate;
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, detail.NumUnit * -1),
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_RESERVED, detail.NumUnit)
        );
        return operation;
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
