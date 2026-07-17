package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleStockConfirmationService {

    @Autowired
    private KardexZoneShared kardexZoneShared;

    @Transactional
    public void consumeReservation(
            SaleHeadEntity saleHead,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) throws SaleException {
        if (saleHead.PresaleCod == null || saleHead.PresaleCod.isBlank()) {
            throw new SaleException("La venta no tiene una preventa reservada asociada");
        }

        for (SaleDetWarehouseEntity detail : detailList) {
            this.validateReservation(saleHead, detail);
            this.kardexZoneShared.apply(this.buildConfirmation(saleHead, detail), userCod);
        }
    }

    public void validateReservation(
            SaleHeadEntity saleHead,
            SaleDetWarehouseEntity detail
    ) throws SaleException {
        List<KardexZoneEntity> reservationList = this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                saleHead.PresaleCod,
                detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        );

        boolean physicalWasSubtracted = reservationList.stream().anyMatch(movement ->
                this.matchesReservationMovement(
                        movement,
                        detail,
                        KardexZoneConstants.ZONE_PHYSICAL,
                        KardexZoneConstants.TYPE_OPERATION_SUBTRACT
                )
        );
        boolean reservedWasAdded = reservationList.stream().anyMatch(movement ->
                this.matchesReservationMovement(
                        movement,
                        detail,
                        KardexZoneConstants.ZONE_RESERVED,
                        KardexZoneConstants.TYPE_OPERATION_ADD
                )
        );

        if (!physicalWasSubtracted || !reservedWasAdded) {
            throw new SaleException(
                    "No existe una reserva valida para el item " + detail.ItemNumber
                            + " de la venta " + saleHead.SaleCod
            );
        }
    }

    private boolean matchesReservationMovement(
            KardexZoneEntity movement,
            SaleDetWarehouseEntity detail,
            String zone,
            String typeOperation
    ) {
        return zone.equals(movement.ZoneStockMoved)
                && typeOperation.equals(movement.TypeOperation)
                && detail.NumUnit == movement.NumStockMoved
                && detail.ProductCod.equals(movement.ProductCod)
                && detail.Variant.equals(movement.Variant)
                && detail.WarehouseCod.equals(movement.WarehouseCod);
    }

    private KardexZoneOperationDto buildConfirmation(
            SaleHeadEntity saleHead,
            SaleDetWarehouseEntity detail
    ) {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = saleHead.SaleCod;
        operation.ItemNumber = detail.ItemNumber;
        operation.SourceTable = SaleConstants.KARDEX_ZONE_SOURCE_SALE;
        operation.MovementEvent = SaleConstants.KARDEX_ZONE_EVENT_CONFIRMATION;
        operation.ProductCod = detail.ProductCod;
        operation.Variant = detail.Variant;
        operation.StoreCod = saleHead.StoreCod;
        operation.WarehouseCod = detail.WarehouseCod;
        operation.LotNumber = detail.LotNumber;
        operation.ExpirationDate = detail.ExpirationDate;
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_RESERVED, detail.NumUnit * -1),
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, detail.NumUnit),
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, detail.NumUnit * -1)
        );
        return operation;
    }
}
