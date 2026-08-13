package com.ccadmin.app.delivery.service;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import org.springframework.stereotype.Service;

@Service
public class BillingIdentityDeliverySearchService {

    private final PersonShared personShared;

    public BillingIdentityDeliverySearchService(PersonShared personShared) {
        this.personShared = personShared;
    }

    public PersonEntity findCompanyByRuc(String ruc) {
        String normalizedRuc = ruc == null ? "" : ruc.replaceAll("\\D", "");
        if (!normalizedRuc.matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe contener 11 digitos");
        }
        return this.personShared.findByDocumentNum("06", normalizedRuc);
    }
}
