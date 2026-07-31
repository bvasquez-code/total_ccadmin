package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.PresaleCancellationDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ExpiredSaleCancellationService extends SessionService {

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
    private KardexShared kardexShared;
    @Autowired
    private KardexZoneRepository kardexZoneRepository;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private CreditNoteApplicationCreateService creditNoteApplicationCreateService;

    public PresaleCancellationDetailDto findCancellationDetail(String presaleCod) throws SaleException {
        PresaleHeadEntity presaleHead = this.presaleHeadRepository.findById(presaleCod)
                .orElseThrow(() -> new SaleException("No existe la preventa " + presaleCod));

        PresaleCancellationDetailDto detail = new PresaleCancellationDetailDto();
        detail.Headboard = presaleHead;
        detail.HasStockReservation = this.hasStockReservation(presaleCod);

        this.saleHeadRepository.findByPresaleCod(presaleCod).ifPresent(saleHead -> {
            detail.SaleDetail = this.saleSearchService.findById(saleHead.SaleCod);
            detail.PendingPaymentAmount = this.pendingPaymentAmount(saleHead.SaleCod);
        });
        return detail;
    }

    @Transactional
    public PresaleCancellationDetailDto cancelPresale(String presaleCod, boolean forced) throws SaleException {
        PresaleHeadEntity presaleHead = this.presaleHeadRepository.findByIdForUpdate(presaleCod)
                .orElseThrow(() -> new SaleException("No existe la preventa " + presaleCod));

        if (StatusConst.CANCELLED.equals(presaleHead.SaleStatus)) {
            return this.findCancellationDetail(presaleCod);
        }
        if (!StatusConst.PENDING.equals(presaleHead.SaleStatus)
                && !StatusConst.CONFIRMED.equals(presaleHead.SaleStatus)) {
            throw new SaleException("La preventa no se encuentra pendiente ni confirmada");
        }

        boolean hasStockReservation = this.hasStockReservation(presaleCod);
        this.validateCancellationMode(presaleHead, forced, hasStockReservation);

        var saleOptional = this.saleHeadRepository.findByPresaleCodForUpdate(presaleCod);
        if (saleOptional.isEmpty()) {
            if (StatusConst.CONFIRMED.equals(presaleHead.SaleStatus)) {
                throw new SaleException("La preventa confirmada no tiene una venta asociada");
            }
            presaleHead.SaleStatus = StatusConst.CANCELLED;
            presaleHead.addSessionModify(getUserCod());
            this.presaleHeadRepository.save(presaleHead);
            return this.findCancellationDetail(presaleCod);
        }

        SaleHeadEntity saleHead = saleOptional.get();
        this.creditNoteApplicationCreateService.releaseBySale(
                saleHead.SaleCod,
                getUserCod()
        );
        this.validateManualSaleCancellation(saleHead);
        this.cancelLockedSale(saleHead, presaleHead, forced, hasStockReservation, getUserCod());
        return this.findCancellationDetail(presaleCod);
    }

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
        this.creditNoteApplicationCreateService.releaseBySale(saleCod, userCod);
        if (this.pendingPaymentAmount(saleCod).signum() > 0) {
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

        this.cancelLockedSale(saleHead, presaleHead, false, true, userCod);
        return true;
    }

    private void validateCancellationMode(
            PresaleHeadEntity presaleHead,
            boolean forced,
            boolean hasStockReservation
    ) throws SaleException {
        if (forced && hasStockReservation) {
            throw new SaleException(
                    "La preventa tiene stock reservado. Debe realizar la anulacion regular"
            );
        }
        if (!forced && StatusConst.CONFIRMED.equals(presaleHead.SaleStatus)
                && !hasStockReservation) {
            throw new SaleException(
                    "La preventa no tiene stock reservado. Use la anulacion forzada"
            );
        }
    }

    private void validateManualSaleCancellation(SaleHeadEntity saleHead) throws SaleException {
        if (!SaleConstants.PENDING.equals(saleHead.SaleStatus)) {
            throw new SaleException("La venta asociada ya no se encuentra pendiente");
        }
        if (this.saleDocumentRepository.findBySaleCod(saleHead.SaleCod) != null) {
            throw new SaleException("La venta ya tiene un documento y no puede anularse");
        }
        if (this.pendingPaymentAmount(saleHead.SaleCod).signum() > 0) {
            throw new SaleException(
                    "La venta pendiente tiene pagos asociados. Debe anularlos antes de continuar"
            );
        }
    }

    private void cancelLockedSale(
            SaleHeadEntity saleHead,
            PresaleHeadEntity presaleHead,
            boolean forced,
            boolean hasStockReservation,
            String userCod
    ) throws SaleException {
        List<KardexZoneEntity> kardexZoneList = List.of();
        if (!forced && hasStockReservation) {
            List<SaleDetWarehouseEntity> detailList =
                    this.saleDetWarehouseRepository.findBySaleCod(saleHead.SaleCod);
            if (detailList.isEmpty()) {
                throw new SaleException("La venta no tiene stock reservado por almacen");
            }
            kardexZoneList = this.kardexShared.buildSaleExpirationRelease(
                    saleHead, detailList, userCod
            );
        }

        saleHead.SaleStatus = SaleConstants.CANCELLED;
        saleHead.IsPaid = "N";
        saleHead.addSessionModify(userCod);
        presaleHead.SaleStatus = StatusConst.CANCELLED;
        presaleHead.IsPaid = "N";
        presaleHead.addSessionModify(userCod);
        this.saleHeadRepository.save(saleHead);
        this.presaleHeadRepository.save(presaleHead);
        if (!kardexZoneList.isEmpty()) {
            this.kardexShared.saveAll(List.of(), kardexZoneList);
        }
    }

    private boolean hasStockReservation(String presaleCod) {
        return this.kardexZoneRepository.countByOperationEvent(
                SaleConstants.KARDEX_ZONE_SOURCE_PRESALE,
                presaleCod,
                SaleConstants.KARDEX_ZONE_EVENT_RESERVATION
        ) > 0;
    }

    private java.math.BigDecimal pendingPaymentAmount(String saleCod) {
        java.math.BigDecimal amount =
                this.salePaymentRepository.findTotalPaymentExcludingCreditNoteApplications(saleCod);
        return amount == null || amount.signum() < 0 ? java.math.BigDecimal.ZERO : amount;
    }

}
