package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.dto.ApplicationInitializationStatusDto;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.CompanyRepository;
import com.ccadmin.app.store.repository.StoreRepository;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInitializationSearchService {

    static final String ROOT_USER_COD = "ROOT";
    static final String DEFAULT_COMPANY_LEGAL_NAME = "COMPANY_DEFAULT";
    static final String DEFAULT_STORE_NAME = "STORE_DEFAULT";

    private final CompanyRepository companyRepository;
    private final StoreRepository storeRepository;

    public ApplicationInitializationSearchService(
            CompanyRepository companyRepository,
            StoreRepository storeRepository) {
        this.companyRepository = companyRepository;
        this.storeRepository = storeRepository;
    }

    public ApplicationInitializationStatusDto findForUser(String userCod) {
        ApplicationInitializationStatusDto status = new ApplicationInitializationStatusDto();
        if (!ROOT_USER_COD.equalsIgnoreCase(userCod)) {
            return status;
        }

        status.CompanyPending = companyRepository
                .findByLegalName(DEFAULT_COMPANY_LEGAL_NAME)
                .isPresent();

        StoreEntity defaultStore = storeRepository
                .findByName(DEFAULT_STORE_NAME)
                .orElse(null);
        status.StorePending = defaultStore != null;
        status.DefaultStoreCod = defaultStore == null ? null : defaultStore.StoreCod;

        // Mantener el flujo activo si uno de los dos registros predeterminados
        // ya fue configurado y el otro todavia sigue pendiente.
        status.Required = status.CompanyPending || status.StorePending;
        return status;
    }
}
