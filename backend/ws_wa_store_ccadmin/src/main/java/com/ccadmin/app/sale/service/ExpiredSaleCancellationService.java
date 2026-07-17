package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.dto.KardexZoneMovementDto;
import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ExpiredSaleCancellationService {

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private PresaleHeadRepository presaleHeadRepository;
    @Autowired
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Autowired
    private SalePaymentRepository salePaymentRepository;
    @Autowired
    private SaleDocumentRepository saleDocumentRepository;
    @Autowired
    private SaleStockConfirmationService saleStockConfirmationService;
    @Autowired
    private KardexZoneShared kardexZoneShared;

    @Transactional
    public boolean cancelExpiredSale(
            String saleCod,
            Date expirationLimit,
            String userCod
    ) throws SaleException {
        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(saleCod)
                .orElseThrow(() -> new SaleException("No existe la venta " + saleCod));

        if (SaleConstants.CANCELLED.equals(saleHead.SaleStatus)) {
            return false;
        }
        if (!SaleConstants.PENDING.equals(saleHead.SaleStatus)) {
            return false;
        }
        if (saleHead.CreationDate == null || saleHead.CreationDate.after(expirationLimit)) {
            return false;
        }
        if (this.salePaymentRepository.countTotalPayment(saleCod) > 0) {
            throw new SaleException("La venta pendiente tiene pagos y requiere gestion manual");
        }
        if (this.saleDocumentRepository.findBySaleCod(saleCod) != null) {
            throw new SaleException("La venta ya tiene un documento y no puede cancelarse automaticamente");
        }
        if (saleHead.PresaleCod == null || saleHead.PresaleCod.isBlank()) {
            throw new SaleException("La venta pendiente no tiene una preventa asociada");
        }

        PresaleHeadEntity presaleHead = this.presaleHeadRepository.findByIdForUpdate(saleHead.PresaleCod)
                .orElseThrow(() -> new SaleException("No existe la preventa " + saleHead.PresaleCod));
        if (!StatusConst.CONFIRMED.equals(presaleHead.SaleStatus)) {
            throw new SaleException("La preventa ya no se encuentra confirmada");
        }

        List<SaleDetWarehouseEntity> detailList =
                this.saleDetWarehouseRepository.findBySaleCod(saleCod);
        if (detailList.isEmpty()) {
            throw new SaleException("La venta no tiene stock reservado por almacen");
        }

        for (SaleDetWarehouseEntity detail : detailList) {
            this.saleStockConfirmationService.validateReservation(saleHead, detail);
            this.kardexZoneShared.apply(this.buildRelease(saleHead, detail), userCod);
        }

        saleHead.SaleStatus = SaleConstants.CANCELLED;
        saleHead.addSessionModify(userCod);
        presaleHead.SaleStatus = StatusConst.CANCELLED;
        presaleHead.addSessionModify(userCod);
        this.saleHeadRepository.save(saleHead);
        this.presaleHeadRepository.save(presaleHead);
        return true;
    }

    private KardexZoneOperationDto buildRelease(
            SaleHeadEntity saleHead,
            SaleDetWarehouseEntity detail
    ) {
        KardexZoneOperationDto operation = new KardexZoneOperationDto();
        operation.OperationCod = saleHead.SaleCod;
        operation.ItemNumber = detail.ItemNumber;
        operation.SourceTable = SaleConstants.KARDEX_ZONE_SOURCE_SALE;
        operation.MovementEvent = SaleConstants.KARDEX_ZONE_EVENT_EXPIRATION_RELEASE;
        operation.ProductCod = detail.ProductCod;
        operation.Variant = detail.Variant;
        operation.StoreCod = saleHead.StoreCod;
        operation.WarehouseCod = detail.WarehouseCod;
        operation.LotNumber = detail.LotNumber;
        operation.ExpirationDate = detail.ExpirationDate;
        operation.MovementList = List.of(
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_RESERVED, detail.NumUnit * -1),
                new KardexZoneMovementDto(KardexZoneConstants.ZONE_PHYSICAL, detail.NumUnit)
        );
        return operation;
    }
}
