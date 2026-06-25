package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.exception.TrxPaymentBuildException;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
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

    public TrxPaymentEntity save(TrxPaymentEntity trxPayment) {
        prepareForSave(trxPayment);
        validatePaymentCreditNote(trxPayment);
        return this.trxPaymentRepository.save(trxPayment);
    }

    public List<TrxPaymentEntity> saveAll(List<TrxPaymentEntity> trxPaymentList) {
        trxPaymentList.forEach(trxPayment -> {
            prepareForSave(trxPayment);
            validatePaymentCreditNote(trxPayment);
        });
        return this.trxPaymentRepository.saveAll(trxPaymentList);
    }

    private void prepareForSave(TrxPaymentEntity trxPayment) {
        trxPayment.addSession(getUserCod());
        trxPayment.validate();

        CurrencyEntity currencySystem = currencyShared.findCurrencySystem();
        trxPayment.CurrencyCodSys = currencySystem.CurrencyCod;
    }

    private void validatePaymentCreditNote(TrxPaymentEntity trxPayment) {
        if (trxPayment.PaymentMethodCod.equals("NC001")) {
            TrxPaymentEntity trxPaymentDB = this.trxPaymentRepository.findByTransactionId(trxPayment.TransactionId);
            if (trxPaymentDB != null && trxPaymentDB.Status.equals("A")) {
                throw new TrxPaymentBuildException("Pago con nota de crédito ya fue usado : " + trxPaymentDB.TransactionId);
            }
        }
    }
}
