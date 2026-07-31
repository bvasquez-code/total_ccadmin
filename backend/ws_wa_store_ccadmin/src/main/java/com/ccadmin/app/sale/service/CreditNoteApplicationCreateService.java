package com.ccadmin.app.sale.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.entity.CreditNoteApplicationEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.repository.CreditNoteApplicationRepository;
import com.ccadmin.app.sale.repository.CreditNoteDocumentRepository;
import com.ccadmin.app.sale.repository.CreditNoteHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class CreditNoteApplicationCreateService extends SessionService {

    private static final String CREDIT_NOTE_PAYMENT_METHOD = "NC001";

    @Autowired
    private CreditNoteHeadRepository creditNoteHeadRepository;
    @Autowired
    private CreditNoteDocumentRepository creditNoteDocumentRepository;
    @Autowired
    private CreditNoteApplicationRepository creditNoteApplicationRepository;
    @Autowired
    private CreditNoteApplicationSearchService creditNoteApplicationSearchService;
    @Autowired
    private TrxPaymentShared trxPaymentShared;
    @Autowired
    private SalePaymentCreateService salePaymentCreateService;
    @Autowired
    private SalePaymentRepository salePaymentRepository;

    @Transactional(rollbackOn = Exception.class)
    public CreditNoteApplicationEntity applyAvailableBalance(
            String creditNoteCod,
            SaleHeadEntity saleHead
    ) throws Exception {
        if (creditNoteCod == null || creditNoteCod.isBlank()) {
            throw new SaleException("El codigo de nota de credito es obligatorio");
        }
        if (saleHead == null || saleHead.SaleCod == null || saleHead.SaleCod.isBlank()) {
            throw new SaleException("La venta para aplicar la nota de credito es obligatoria");
        }

        CreditNoteHeadEntity creditNoteHead = this.creditNoteHeadRepository
                .findByIdForUpdate(creditNoteCod)
                .orElseThrow(() -> new SaleException("No existe la nota de credito " + creditNoteCod));
        validateApplication(creditNoteHead, saleHead);

        BigDecimal availableBalance =
                this.creditNoteApplicationSearchService.findAvailableBalance(creditNoteHead);
        if (availableBalance.signum() <= 0) {
            throw new SaleException("La nota de credito ya no tiene saldo disponible");
        }
        if (saleHead.NumTotalPrice.compareTo(availableBalance) < 0) {
            throw new SaleException(
                    "El total de la nueva venta debe ser igual o mayor al saldo de la nota de credito"
            );
        }

        CreditNoteDocumentEntity document =
                this.creditNoteDocumentRepository.findByCreditNoteCod(creditNoteCod);
        if (document == null) {
            throw new SaleException("La nota de credito no tiene documento confirmado");
        }

        TrxPaymentEntity trxPayment = buildCreditPayment(
                creditNoteHead,
                saleHead,
                document,
                availableBalance
        );
        trxPayment = this.trxPaymentShared.saveCreditNoteApplication(trxPayment);

        SalePaymentRegisterDto salePaymentRegister = new SalePaymentRegisterDto();
        salePaymentRegister.SaleCod = saleHead.SaleCod;
        salePaymentRegister.TrxPaymentId = trxPayment.TrxPaymentId;
        this.salePaymentCreateService.save(salePaymentRegister);

        CreditNoteApplicationEntity application = new CreditNoteApplicationEntity();
        application.CreditNoteCod = creditNoteCod;
        application.SaleCod = saleHead.SaleCod;
        application.TrxPaymentId = trxPayment.TrxPaymentId;
        application.AmountApplied = availableBalance;
        application.session(getUserCod());
        application = this.creditNoteApplicationRepository.save(application);

        creditNoteHead.IsPaid = "S";
        creditNoteHead.addSessionModify(getUserCod());
        this.creditNoteHeadRepository.save(creditNoteHead);
        return application;
    }

    @Transactional(rollbackOn = Exception.class)
    public int releaseBySale(String saleCod, String userCod) throws SaleException {
        List<CreditNoteApplicationEntity> applicationList =
                this.creditNoteApplicationSearchService.findActiveBySaleCod(saleCod);
        if (applicationList.isEmpty()) {
            return 0;
        }

        List<SalePaymentEntity> salePaymentList =
                this.salePaymentRepository.findBySaleCod(saleCod);
        for (CreditNoteApplicationEntity application : applicationList) {
            SalePaymentEntity salePayment = salePaymentList.stream()
                    .filter(item -> item.TrxPaymentId == application.TrxPaymentId)
                    .findFirst()
                    .orElseThrow(() -> new SaleException(
                            "La aplicacion de nota no tiene pago asociado en la venta"
                    ));

            salePayment.inactive(userCod);
            this.salePaymentRepository.save(salePayment);
            this.trxPaymentShared.inactivateCreditNoteApplication(
                    application.TrxPaymentId,
                    userCod
            );

            application.inactive(userCod);
            this.creditNoteApplicationRepository.save(application);

            CreditNoteHeadEntity creditNoteHead = this.creditNoteHeadRepository
                    .findByIdForUpdate(application.CreditNoteCod)
                    .orElseThrow(() -> new SaleException(
                            "No existe la nota de credito " + application.CreditNoteCod
                    ));
            creditNoteHead.IsPaid = "N";
            creditNoteHead.addSessionModify(userCod);
            this.creditNoteHeadRepository.save(creditNoteHead);
        }
        return applicationList.size();
    }

    private void validateApplication(
            CreditNoteHeadEntity creditNoteHead,
            SaleHeadEntity saleHead
    ) throws SaleException {
        if (!SaleConstants.CONFIRMED.equals(creditNoteHead.CreditNoteStatus)) {
            throw new SaleException("La nota de credito debe estar confirmada");
        }
        if (!StatusConst.ACTIVE.equals(creditNoteHead.Status)) {
            throw new SaleException("La nota de credito no se encuentra activa");
        }
        if (!"S".equals(creditNoteHead.IsProductExchange)) {
            throw new SaleException("La nota de credito no corresponde a un cambio de producto");
        }
        if (!SaleConstants.PENDING.equals(saleHead.SaleStatus)) {
            throw new SaleException("La nueva venta debe encontrarse pendiente");
        }
        if (!Objects.equals(creditNoteHead.StoreCod, saleHead.StoreCod)) {
            throw new SaleException("La nota de credito pertenece a otra tienda");
        }
        if (!Objects.equals(creditNoteHead.CurrencyCod, saleHead.CurrencyCod)) {
            throw new SaleException("La nota de credito y la nueva venta deben tener la misma moneda");
        }
        if (creditNoteHead.ClientCod != null
                && !creditNoteHead.ClientCod.isBlank()
                && !Objects.equals(creditNoteHead.ClientCod, saleHead.ClientCod)) {
            throw new SaleException("La nueva venta debe pertenecer al cliente de la nota de credito");
        }
    }

    private TrxPaymentEntity buildCreditPayment(
            CreditNoteHeadEntity creditNoteHead,
            SaleHeadEntity saleHead,
            CreditNoteDocumentEntity document,
            BigDecimal availableBalance
    ) {
        TrxPaymentEntity trxPayment = new TrxPaymentEntity();
        trxPayment.PaymentMethodCod = CREDIT_NOTE_PAYMENT_METHOD;
        trxPayment.PaymentPlatform = "CREDITO_INTERNO";
        trxPayment.TransactionId = document.CounterfoilCod + "-" + document.DocumentCod;
        trxPayment.PaymentStatus = "OK";
        trxPayment.CurrencyCod = creditNoteHead.CurrencyCod;
        trxPayment.CurrencyCodSys = saleHead.CurrencyCodSys;
        trxPayment.NumExchangevalue = saleHead.NumExchangevalue;
        trxPayment.AmountPaid = availableBalance;
        trxPayment.AmountReturned = BigDecimal.ZERO;
        trxPayment.TypeMovement = "I";
        trxPayment.ReversalOfTrxPaymentId = null;
        return trxPayment;
    }
}
