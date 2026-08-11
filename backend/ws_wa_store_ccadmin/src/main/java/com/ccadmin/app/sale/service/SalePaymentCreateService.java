package com.ccadmin.app.sale.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.sale.exception.SalePaymentException;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.dto.SalesContextDto;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.model.factory.SalePaymentEntityFactory;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
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
public class SalePaymentCreateService extends SessionService {

    @Autowired
    private SalePaymentRepository salePaymentRepository;
    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private TrxPaymentShared trxPaymentShared;
    @Autowired
    private SalesContextService salesContextService;

    @Transactional
    public SalePaymentEntity save(SalePaymentRegisterDto payment) throws Exception {
        return save(payment, salesContextService.getInternalContext());
    }

    @Transactional
    public SalePaymentEntity saveWeb(SalePaymentRegisterDto payment, String storeCod) throws Exception {
        return save(payment, salesContextService.getWebContext(storeCod));
    }

    private SalePaymentEntity save(
            SalePaymentRegisterDto payment,
            SalesContextDto salesContext
    ) throws Exception {
        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(payment.SaleCod)
                .orElseThrow(() -> new SalePaymentException("Sale does not exist"));

        if (!salesContext.StoreCod.equals(saleHead.StoreCod)) {
            throw new SalePaymentException("La venta no pertenece a la tienda indicada");
        }

        if(!StatusConst.PENDING.equals(saleHead.SaleStatus)){
            throw new SalePaymentException("Sale is no longer pending");
        }

        TrxPaymentEntity trxPayment = this.trxPaymentShared.findById(payment.TrxPaymentId);
        BigDecimal TotalPayment = this.salePaymentRepository.findTotalPayment(payment.SaleCod);
        if( TotalPayment.doubleValue() >= saleHead.NumTotalPrice.doubleValue() ) {
            throw new SalePaymentException("Sale is completed payment");
        }

        BigDecimal NumAmountPaid = trxPayment.AmountPaid.multiply(saleHead.NumExchangevalue);
        BigDecimal NumAmountReturned = TotalPayment.add(NumAmountPaid).subtract(saleHead.NumTotalPrice);

        SalePaymentEntity salePayment = SalePaymentEntityFactory.fromTransaction(
                saleHead,
                trxPayment,
                this.salePaymentRepository.countTotalPayment(payment.SaleCod),
                NumAmountPaid,
                NumAmountReturned
        );
        salePayment.addSession(salesContext.UserCod);

        salePaymentRepository.save(salePayment);
        if( TotalPayment.add(salePayment.NumAmountPaid).doubleValue() >= saleHead.NumTotalPrice.doubleValue() ){

            saleHead.IsPaid = "S";
            saleHead.addSession(salesContext.UserCod);
            this.saleHeadRepository.save(saleHead);

            // this.saleCreateService.confirm(payment.SaleCod,payment.DocumentType,payment.CounterfoilCod);
        }
        return salePayment;
    }

    @Transactional
    public SalePaymentEntity saveReversal(SalePaymentRegisterDto payment) throws Exception {
        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(payment.SaleCod)
                .orElseThrow(() -> new SalePaymentException("La venta no existe"));
        if (!StatusConst.PENDING.equals(saleHead.SaleStatus)) {
            throw new SalePaymentException("La venta ya no se encuentra pendiente");
        }

        TrxPaymentEntity reversal = this.trxPaymentShared.findById(payment.TrxPaymentId);
        if (!"E".equals(reversal.TypeMovement) || reversal.ReversalOfTrxPaymentId == null) {
            throw new SalePaymentException("La transaccion de pago no corresponde a una reversa");
        }

        List<SalePaymentEntity> paymentList = this.salePaymentRepository.findBySaleCod(payment.SaleCod);
        if (paymentList.stream().anyMatch(item -> item.TrxPaymentId == reversal.TrxPaymentId)) {
            throw new SalePaymentException("La reversa de pago ya fue registrada");
        }

        SalePaymentEntity originalPayment = paymentList.stream()
                .filter(item -> item.TrxPaymentId == reversal.ReversalOfTrxPaymentId)
                .findFirst()
                .orElseThrow(() -> new SalePaymentException("El pago original no pertenece a la venta"));

        BigDecimal alreadyReversed = BigDecimal.ZERO;
        for (SalePaymentEntity item : paymentList) {
            TrxPaymentEntity registeredPayment = this.trxPaymentShared.findById(item.TrxPaymentId);
            if ("E".equals(registeredPayment.TypeMovement)
                    && Objects.equals(registeredPayment.ReversalOfTrxPaymentId, originalPayment.TrxPaymentId)) {
                alreadyReversed = alreadyReversed.add(item.NumAmountPaid.abs());
            }
        }

        BigDecimal exchangeValue = originalPayment.NumExchangevalue == null
                ? BigDecimal.ONE
                : originalPayment.NumExchangevalue;
        BigDecimal reversalAmount = reversal.AmountPaid.abs().multiply(exchangeValue);
        BigDecimal reversibleAmount = originalPayment.NumAmountPaid.abs().subtract(alreadyReversed);
        if (reversalAmount.signum() <= 0 || reversalAmount.compareTo(reversibleAmount) > 0) {
            throw new SalePaymentException("El monto de reversa supera el saldo del pago original");
        }

        SalePaymentEntity salePayment = SalePaymentEntityFactory.fromReversal(
                originalPayment,
                reversal,
                getUserCod(),
                paymentList.size() + 1
        );
        salePayment.NumAmountPaid = reversal.AmountPaid.multiply(exchangeValue);
        salePayment.NumAmountPaidOrigin = reversal.AmountPaid;
        return this.salePaymentRepository.save(salePayment);
    }

    @Transactional
    public SalePaymentEntity save(SalePaymentEntity salePayment){
        return this.salePaymentRepository.save(salePayment);
    }

    @Transactional
    public List<SalePaymentEntity> saveAll(List<SalePaymentEntity> salePaymentList){
        return this.salePaymentRepository.saveAll(salePaymentList);
    }

}
