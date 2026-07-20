package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.shared.KardexZoneShared;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<KardexZoneEntity> kardexZoneList = new ArrayList<>();
        Map<String, ProductInfoWarehouseEntity> stockCursorByWarehouse = new HashMap<>();
        for (SaleDetWarehouseEntity detail : detailList) {
            this.validateReservation(saleHead, detail);
            if (this.kardexZoneShared.isApplied(
                    SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                    saleHead.SaleCod, detail.ItemNumber,
                    SaleConstants.KARDEX_ZONE_EVENT_EXPIRATION_RELEASE
            )) {
                continue;
            }
            String key = detail.ProductCod + "|" + detail.Variant + "|"
                    + saleHead.StoreCod + "|" + detail.WarehouseCod;
            ProductInfoWarehouseEntity stockCursor = stockCursorByWarehouse.computeIfAbsent(
                    key,
                    ignored -> this.kardexZoneShared.findStockForUpdate(
                            detail.ProductCod, detail.Variant,
                            saleHead.StoreCod, detail.WarehouseCod
                    )
            );
            kardexZoneList.addAll(KardexZoneEntity.buildSaleExpirationRelease(
                    saleHead, detail, stockCursor, userCod
            ));
        }
        this.kardexZoneShared.saveAll(kardexZoneList);

        saleHead.SaleStatus = SaleConstants.CANCELLED;
        saleHead.addSessionModify(userCod);
        presaleHead.SaleStatus = StatusConst.CANCELLED;
        presaleHead.addSessionModify(userCod);
        this.saleHeadRepository.save(saleHead);
        this.presaleHeadRepository.save(presaleHead);
        return true;
    }

    private void validateReservation(
            SaleHeadEntity saleHead,
            SaleDetWarehouseEntity detail
    ) throws SaleException {
        List<KardexZoneEntity> reservationList = this.kardexZoneShared.findByEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                saleHead.PresaleCod,
                detail.ItemNumber,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        );
        if (!KardexZoneEntity.isValidPresaleReservation(reservationList, detail)) {
            throw new SaleException(
                    "No existe una reserva valida para el item " + detail.ItemNumber
                            + " de la venta " + saleHead.SaleCod
            );
        }
    }

}
