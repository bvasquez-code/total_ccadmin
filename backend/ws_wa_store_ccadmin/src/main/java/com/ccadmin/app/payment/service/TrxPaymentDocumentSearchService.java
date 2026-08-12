package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentDocumentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrxPaymentDocumentSearchService {

    private final TrxPaymentDocumentRepository trxPaymentDocumentRepository;

    public TrxPaymentDocumentSearchService(
            TrxPaymentDocumentRepository trxPaymentDocumentRepository
    ) {
        this.trxPaymentDocumentRepository = trxPaymentDocumentRepository;
    }

    public List<TrxPaymentDocumentEntity> findActiveByTrxPaymentId(Long trxPaymentId) {
        if (trxPaymentId == null || trxPaymentId <= 0) {
            throw new IllegalArgumentException("El identificador del pago es obligatorio");
        }
        return this.trxPaymentDocumentRepository.findActiveByTrxPaymentId(trxPaymentId);
    }
}
