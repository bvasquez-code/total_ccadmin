package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.service.CompanySearchService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

@Service
public class SunatInitializationSearchService extends SessionService {
    private final SunatConfigurationClientService sunatConfigurationClientService;
    private final CompanySearchService companySearchService;

    public SunatInitializationSearchService(SunatConfigurationClientService sunatConfigurationClientService,
                                           CompanySearchService companySearchService) {
        this.sunatConfigurationClientService = sunatConfigurationClientService;
        this.companySearchService = companySearchService;
    }

    public ObjectNode findDataForm() {
        requireRoot();
        ObjectNode form = sunatConfigurationClientService.findInitializationForm();
        String issuerRuc = issuerRuc();
        if (!issuerRuc.equals(form.path("IssuerRuc").asText())) {
            form.put("HasSolPassword", false);
            form.put("HasCertificate", false);
            form.put("HasCertificatePassword", false);
            form.put("HasGuideClientSecret", false);
        }
        form.put("IssuerRuc", issuerRuc);
        return form;
    }

    public String issuerRuc() {
        var company = companySearchService.findMyCompany();
        if (company == null || company.TaxId == null || !company.TaxId.matches("\\d{11}")) {
            throw new IllegalArgumentException("Configure primero el RUC de la compañía");
        }
        return company.TaxId;
    }

    public void requireRoot() {
        if (!"ROOT".equalsIgnoreCase(getUserCod())) {
            throw new IllegalArgumentException("Solo ROOT puede completar la configuración inicial de SUNAT");
        }
    }
}
