package com.ccadmin.app.payment.shared;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.service.TrxPaymentCreateService;
import com.ccadmin.app.payment.service.TrxPaymentSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrxPaymentShared {
    @Autowired
    private TrxPaymentCreateService trxPaymentCreateService;

    @Autowired
    private TrxPaymentSearchService trxPaymentSearchService;

    public TrxPaymentEntity save(TrxPaymentEntity trxPayment) {
        return this.trxPaymentCreateService.save(trxPayment);
    }

    public List<TrxPaymentEntity> saveAll(List<TrxPaymentEntity> trxPaymentList) {
        return this.trxPaymentCreateService.saveAll(trxPaymentList);
    }

    public TrxPaymentEntity findById(Long TrxPaymentId){
        return this.trxPaymentSearchService.findById(TrxPaymentId);
    }

    public List<TrxPaymentEntity> findAllById(List<Long> TrxPaymentId){
        return this.trxPaymentSearchService.findAllById(TrxPaymentId);
    }
}
