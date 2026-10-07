package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.exception.TrxPaymentBuildException;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrxPaymentCreateService extends SessionService {

    @Autowired
    private TrxPaymentRepository trxPaymentRepository;

    @Autowired
    private CurrencyShared currencyShared;

    @Autowired
    private PinpadPaymentCreateService pinpadPaymentCreateService;

    public TrxPaymentEntity save(TrxPaymentEntity trxPayment) {
        return this.save(trxPayment, getUserCod(), getCashSessionID());
    }

    public TrxPaymentEntity saveWeb(TrxPaymentEntity trxPayment) {
        return this.save(trxPayment, AuditUserConstants.USER_WEB, null);
    }

    public com.ccadmin.app.payment.model.dto.PinpadBrowserInstructionsDto preparePinpad(TrxPaymentEntity trxPayment) {
        String userCod = getUserCod();
        Long cashSessionID = getCashSessionID();
        rejectManualCreditNotePayment(trxPayment);
        prepareForSave(trxPayment, userCod, cashSessionID);
        return pinpadPaymentCreateService.prepare(trxPayment, userCod, cashSessionID, getStoreCod());
    }

    private TrxPaymentEntity save(
            TrxPaymentEntity trxPayment,
            String userCod,
            Long cashSessionID
    ) {
        rejectManualCreditNotePayment(trxPayment);
        prepareForSave(trxPayment, userCod, cashSessionID);
        validatePaymentCreditNote(trxPayment);
        return savePrepared(trxPayment, userCod, cashSessionID);
    }

    public List<TrxPaymentEntity> saveAll(List<TrxPaymentEntity> trxPaymentList) {
        String userCod = getUserCod();
        Long cashSessionID = getCashSessionID();
        trxPaymentList.forEach(trxPayment -> {
            rejectManualCreditNotePayment(trxPayment);
            prepareForSave(trxPayment, userCod, cashSessionID);
            validatePaymentCreditNote(trxPayment);
        });
        if (trxPaymentList.stream().anyMatch(PinpadPaymentCreateService::requiresPinpad)) {
            // Each physical approval commits independently and can be recovered by the same reference.
            return trxPaymentList.stream()
                    .map(payment -> savePrepared(payment, userCod, cashSessionID)).toList();
        }
        return this.trxPaymentRepository.saveAll(trxPaymentList);
    }

    private TrxPaymentEntity savePrepared(TrxPaymentEntity trxPayment, String userCod, Long cashSessionID) {
        if (PinpadPaymentCreateService.requiresPinpad(trxPayment)) {
            return pinpadPaymentCreateService.pay(trxPayment, userCod, cashSessionID, getStoreCod());
        }
        return this.trxPaymentRepository.save(trxPayment);
    }

    public TrxPaymentEntity saveCreditNoteApplication(TrxPaymentEntity trxPayment) {
        if (!"NC001".equals(trxPayment.PaymentMethodCod) || !"I".equals(trxPayment.TypeMovement)) {
            throw new TrxPaymentBuildException(
                    "La transaccion interna debe corresponder a una aplicacion de nota de credito"
            );
        }
        prepareForSave(trxPayment, getUserCod(), getCashSessionID());
        validatePaymentCreditNote(trxPayment);
        return this.trxPaymentRepository.save(trxPayment);
    }

    public TrxPaymentEntity inactivateCreditNoteApplication(
            Long trxPaymentId,
            String userCod
    ) {
        TrxPaymentEntity trxPayment = this.trxPaymentRepository.findById(trxPaymentId)
                .orElseThrow(() -> new TrxPaymentBuildException(
                        "No existe la transaccion interna de nota de credito"
                ));
        if (!"NC001".equals(trxPayment.PaymentMethodCod)) {
            throw new TrxPaymentBuildException(
                    "La transaccion no corresponde a una aplicacion de nota de credito"
            );
        }
        trxPayment.inactive(userCod);
        return this.trxPaymentRepository.save(trxPayment);
    }

    private void prepareForSave(
            TrxPaymentEntity trxPayment,
            String userCod,
            Long cashSessionID
    ) {
        if (trxPayment.TrxPaymentId != null && trxPayment.TrxPaymentId > 0
                && !PinpadPaymentCreateService.requiresPinpad(trxPayment)) {
            this.trxPaymentRepository.findById(trxPayment.TrxPaymentId).ifPresent(existing -> {
                if (existing.PinpadPaymentId != null) {
                    throw new TrxPaymentBuildException("No se puede sobrescribir un pago confirmado por pinpad");
                }
            });
        }
        if (!PinpadPaymentCreateService.requiresPinpad(trxPayment)
                && trxPayment.PinpadPaymentId != null && !trxPayment.PinpadPaymentId.isBlank()) {
            throw new TrxPaymentBuildException("La referencia pinpad solo puede guardarse mediante un pago POS");
        }
        trxPayment.CashSessionID = cashSessionID;
        trxPayment.addSession(userCod);
        trxPayment.validate();

        CurrencyEntity currencySystem = currencyShared.findCurrencySystem();
        trxPayment.CurrencyCodSys = currencySystem.CurrencyCod;
    }

    private void rejectManualCreditNotePayment(TrxPaymentEntity trxPayment) {
        if ("NC001".equals(trxPayment.PaymentMethodCod) && "I".equals(trxPayment.TypeMovement)) {
            throw new TrxPaymentBuildException(
                    "La nota de credito se aplica automaticamente desde el cambio de producto"
            );
        }
    }

    private void validatePaymentCreditNote(TrxPaymentEntity trxPayment) {
        if ("NC001".equals(trxPayment.PaymentMethodCod) && "I".equals(trxPayment.TypeMovement)) {
            TrxPaymentEntity trxPaymentDB = this.trxPaymentRepository.findByTransactionId(trxPayment.TransactionId);
            if (trxPaymentDB != null && trxPaymentDB.Status.equals("A")) {
                throw new TrxPaymentBuildException("Pago con nota de crédito ya fue usado : " + trxPaymentDB.TransactionId);
            }
        }
    }
}
