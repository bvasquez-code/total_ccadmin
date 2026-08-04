package com.ccadmin.app.sunat.identity.service;

import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.sunat.identity.provider.IdentityQueryProvider;
import org.springframework.stereotype.Service;

@Service
public class SunatIdentitySearchService {

    private final IdentityQueryProvider identityQueryProvider;

    public SunatIdentitySearchService(IdentityQueryProvider identityQueryProvider) {
        this.identityQueryProvider = identityQueryProvider;
    }

    public CompanyIdentityResponseDto findCompanyByRuc(String ruc) {
        String normalizedRuc = normalizeRuc(ruc);
        return this.identityQueryProvider.findCompanyByRuc(normalizedRuc);
    }

    public PersonIdentityResponseDto findPersonByDocument(
            String documentTypeReference,
            String documentNumber
    ) {
        IdentityDocumentType documentType = IdentityDocumentType.fromReference(documentTypeReference);
        String normalizedDocumentNumber = documentType.normalizeDocumentNumber(documentNumber);
        return this.identityQueryProvider.findPersonByDocument(documentType, normalizedDocumentNumber);
    }

    private String normalizeRuc(String ruc) {
        if (ruc == null || !ruc.trim().matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe contener exactamente 11 dígitos.");
        }
        return ruc.trim();
    }
}
