package com.ccadmin.app.sale.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.sale.exception.SalePaymentException;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SalePaymentCreateService extends SessionService {

    @Autowired
    private SalePaymentRepository salePaymentRepository;
    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private TrxPaymentShared trxPaymentShared;
    @Autowired
    private SaleCreateService saleCreateService;

    @Transactional
    public SalePaymentEntity save(SalePaymentRegisterDto payment) throws Exception {

        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(payment.SaleCod)
                .orElseThrow(() -> new SalePaymentException("Sale does not exist"));

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

        SalePaymentEntity salePayment = new SalePaymentEntity(
                 saleHead
                ,trxPayment
                ,this.salePaymentRepository.countTotalPayment(payment.SaleCod)
                ,NumAmountPaid
                ,NumAmountReturned
        ).build();
        salePayment.addSession(getUserCod());

        salePaymentRepository.save(salePayment);
        if( TotalPayment.add(salePayment.NumAmountPaid).doubleValue() >= saleHead.NumTotalPrice.doubleValue() ){

            saleHead.IsPaid = "S";
            saleHead.addSession(getUserCod());
            this.saleHeadRepository.save(saleHead);

            // this.saleCreateService.confirm(payment.SaleCod,payment.DocumentType,payment.CounterfoilCod);
        }
        return salePayment;
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
