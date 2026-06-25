package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import com.ccadmin.app.system.repository.PaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentMethodService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodEntity findById(String PaymentMethodCod)
    {
        return this.paymentMethodRepository.findById(PaymentMethodCod).get();
    }
    public List<PaymentMethodEntity> findAllActive()
    {
        return this.paymentMethodRepository.findAllActive();
    }

}
