package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.dto.PinpadPaymentStatusDto;
import com.ccadmin.app.payment.model.entity.TrxPaymentDocumentEntity;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrxPaymentPinpadCreateService {
    private final TrxPaymentRepository trxPaymentRepository;
    private final TrxPaymentDocumentCreateService trxPaymentDocumentCreateService;
    private final ObjectMapper objectMapper;

    public TrxPaymentPinpadCreateService(TrxPaymentRepository trxPaymentRepository,
                                        TrxPaymentDocumentCreateService trxPaymentDocumentCreateService,
                                        ObjectMapper objectMapper) {
        this.trxPaymentRepository = trxPaymentRepository;
        this.trxPaymentDocumentCreateService = trxPaymentDocumentCreateService;
        this.objectMapper = objectMapper;
    }

    // Commit the approved payment and its evidence before acknowledging the local agent.
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TrxPaymentEntity saveApproved(TrxPaymentEntity payment, PinpadPaymentStatusDto result) {
        if (!"APPROVED".equals(result.status())) {
            throw new IllegalArgumentException("Solo se puede guardar un pago pinpad aprobado");
        }
        payment.TrxPaymentId = null;
        payment.Status = "A";
        payment.PaymentStatus = "OK";
        payment.TransactionId = result.transactionId();
        payment.CardNumber = result.lastFour() != null && result.lastFour().matches("[0-9]{4}")
                ? "************" + result.lastFour() : null;
        payment.CardCVV = null;
        payment.CardExpirationDate = null;
        payment.CardHolderName = null;
        TrxPaymentEntity saved = trxPaymentRepository.saveAndFlush(payment);

        TrxPaymentDocumentEntity document = new TrxPaymentDocumentEntity();
        document.DocumentType = "PINPAD_RESPONSE";
        document.SourceType = "PINPAD";
        document.ContentEncoding = "JSON";
        document.ContentType = "application/json";
        document.FileName = "pinpad-" + saved.TrxPaymentId + ".json";
        try {
            document.Content = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo preparar el comprobante pinpad");
        }
        trxPaymentDocumentCreateService.save(saved.TrxPaymentId, List.of(document));
        return saved;
    }
}
