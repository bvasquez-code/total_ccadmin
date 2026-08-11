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
    @Autowired
    private AppFilePublicUrlService appFilePublicUrlService;

    public PaymentMethodEntity findById(String PaymentMethodCod)
    {
        return appFilePublicUrlService.applyPublicRoute(
                this.paymentMethodRepository.findById(PaymentMethodCod).get()
        );
    }
    public List<PaymentMethodEntity> findAllActive()
    {
        return this.applyPublicRoutes(this.paymentMethodRepository.findAllActive());
    }

    public List<PaymentMethodEntity> findAllActiveInternalSale()
    {
        return this.applyPublicRoutes(this.paymentMethodRepository.findAllActiveInternalSale());
    }

    public List<PaymentMethodEntity> findAllActiveWebSale()
    {
        return this.applyPublicRoutes(this.paymentMethodRepository.findAllActiveWebSale());
    }

    private List<PaymentMethodEntity> applyPublicRoutes(List<PaymentMethodEntity> paymentMethodList)
    {
        paymentMethodList.forEach(this.appFilePublicUrlService::applyPublicRoute);
        return paymentMethodList;
    }

}
