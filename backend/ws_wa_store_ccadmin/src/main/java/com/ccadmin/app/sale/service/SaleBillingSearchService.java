package com.ccadmin.app.sale.service;

import com.ccadmin.app.person.shared.PersonShared;
import com.ccadmin.app.sale.model.entity.SaleBillingEntity;
import com.ccadmin.app.sale.repository.SaleBillingRepository;
import org.springframework.stereotype.Service;

@Service
public class SaleBillingSearchService {

    private final SaleBillingRepository saleBillingRepository;
    private final PersonShared personShared;

    public SaleBillingSearchService(
            SaleBillingRepository saleBillingRepository,
            PersonShared personShared
    ) {
        this.saleBillingRepository = saleBillingRepository;
        this.personShared = personShared;
    }

    public SaleBillingEntity findBySaleCod(String saleCod) {
        SaleBillingEntity saleBilling = this.saleBillingRepository.findActiveBySaleCod(saleCod)
                .orElse(null);
        if (saleBilling != null && saleBilling.PersonCod != null && !saleBilling.PersonCod.isBlank()) {
            try {
                saleBilling.Person = this.personShared.findById(saleBilling.PersonCod);
            } catch (RuntimeException ignored) {
                saleBilling.Person = null;
            }
        }
        return saleBilling;
    }
}
