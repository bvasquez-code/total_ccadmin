package com.ccadmin.app.delivery.service;

import com.ccadmin.app.identity.model.dto.CompanyIdentityDto;
import com.ccadmin.app.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.identity.service.SunatIdentitySearchService;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BillingIdentityDeliverySearchService {

    private final PersonShared personShared;
    private final SunatIdentitySearchService sunatIdentitySearchService;

    public BillingIdentityDeliverySearchService(
            PersonShared personShared,
            SunatIdentitySearchService sunatIdentitySearchService
    ) {
        this.personShared = personShared;
        this.sunatIdentitySearchService = sunatIdentitySearchService;
    }

    public PersonEntity findCompanyByRuc(String ruc) {
        String normalizedRuc = ruc == null ? "" : ruc.replaceAll("\\D", "");
        if (!normalizedRuc.matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe contener 11 digitos");
        }

        try {
            CompanyIdentityResponseDto identityResponse = sunatIdentitySearchService
                    .findCompanyByRuc(normalizedRuc);
            if (identityResponse != null
                    && identityResponse.found()
                    && identityResponse.company() != null
                    && hasText(identityResponse.company().legalName())) {
                return mapCompany(identityResponse.company(), normalizedRuc);
            }
        } catch (IllegalStateException exception) {
            log.warn(
                    "No fue posible consultar la identidad SUNAT; se usara la fuente interna: {}",
                    exception.getMessage()
            );
        }

        return this.personShared.findByDocumentNum("06", normalizedRuc);
    }

    private PersonEntity mapCompany(CompanyIdentityDto company, String ruc) {
        PersonEntity person = new PersonEntity();
        person.PersonType = "04";
        person.DocumentType = "06";
        person.DocumentNum = ruc;
        person.Names = "-";
        person.LastNames = "-";
        person.BusinessName = clean(company.legalName());
        person.CommercialName = clean(company.tradeName());
        person.Address = clean(company.fiscalAddress());
        return person;
    }

    private boolean hasText(String value) {
        return !clean(value).isEmpty();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
