package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.dto.ApplicationInitializationStatusDto;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.CompanyRepository;
import com.ccadmin.app.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationInitializationSearchServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private StoreRepository storeRepository;

    private ApplicationInitializationSearchService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationInitializationSearchService(companyRepository, storeRepository);
    }

    @Test
    void doesNotRequestInitializationForUsersOtherThanRoot() {
        ApplicationInitializationStatusDto status = service.findForUser("ADMIN");

        assertFalse(status.Required);
        assertFalse(status.CompanyPending);
        assertFalse(status.StorePending);
        assertNull(status.DefaultStoreCod);
        verifyNoInteractions(companyRepository, storeRepository);
    }

    @Test
    void requestsInitializationWhenBothDefaultRecordsExist() {
        CompanyEntity company = new CompanyEntity();
        StoreEntity store = new StoreEntity();
        store.StoreCod = "T001";
        when(companyRepository.findByLegalName("COMPANY_DEFAULT"))
                .thenReturn(Optional.of(company));
        when(storeRepository.findByName("STORE_DEFAULT"))
                .thenReturn(Optional.of(store));

        ApplicationInitializationStatusDto status = service.findForUser("root");

        assertTrue(status.Required);
        assertTrue(status.CompanyPending);
        assertTrue(status.StorePending);
        assertEquals("T001", status.DefaultStoreCod);
    }

    @Test
    void keepsInitializationActiveWhileTheDefaultStoreRemains() {
        StoreEntity store = new StoreEntity();
        store.StoreCod = "T001";
        when(companyRepository.findByLegalName("COMPANY_DEFAULT"))
                .thenReturn(Optional.empty());
        when(storeRepository.findByName("STORE_DEFAULT"))
                .thenReturn(Optional.of(store));

        ApplicationInitializationStatusDto status = service.findForUser("ROOT");

        assertTrue(status.Required);
        assertFalse(status.CompanyPending);
        assertTrue(status.StorePending);
        assertEquals("T001", status.DefaultStoreCod);
    }

    @Test
    void reportsAnInitializedApplicationWithoutDefaultRecords() {
        when(companyRepository.findByLegalName("COMPANY_DEFAULT"))
                .thenReturn(Optional.empty());
        when(storeRepository.findByName("STORE_DEFAULT"))
                .thenReturn(Optional.empty());

        ApplicationInitializationStatusDto status = service.findForUser("ROOT");

        assertFalse(status.Required);
        assertFalse(status.CompanyPending);
        assertFalse(status.StorePending);
        assertNull(status.DefaultStoreCod);
    }
}
