package com.ccadmin.app.security.service;

import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.security.model.dto.SessionStorageDto;
import com.ccadmin.app.security.model.dto.ApplicationInitializationStatusDto;
import com.ccadmin.app.security.model.entity.AppSessionEntity;
import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.security.repository.AppUserRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.user.shared.AppMenuShared;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityService extends SessionService {
    @Autowired
    private com.ccadmin.app.store.shared.StoreShared storeShared;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private AppMenuShared appMenuShared;
    @Autowired
    private CashSessionRepository cashSessionRepository;
    @Autowired
    private ApplicationInitializationSearchService applicationInitializationSearchService;

    @Transactional
    public void createUserSession(String userCod, String token) {
        String storeCod = userStoreShared.getMainStore(userCod);
        Long cashSessionId = cashSessionRepository.findOpenIdByUserAndStore(userCod, storeCod)
                .orElse(null);
        AppSessionEntity session = new AppSessionEntity(userCod, token, cashSessionId);
        if (userStoreShared.findByUserCod(userCod).size() == 1) session.selectStore(storeCod);
        else session.CashSessionID = null;
        appSessionRepository.save(session);
    }

    @Transactional
    public SessionStorageDto findUserSession() {

        SessionStorageDto sessionStorage = new SessionStorageDto();
        sessionStorage.UserCod = getUserCod();

        AppSessionEntity appSession = this.appSessionRepository.findActiveBySessionId(getSessionID())
                .orElseThrow(() -> new IllegalStateException("La sesión autenticada ya no se encuentra activa"));
        AppUserEntity appUser = this.appUserRepository.findById(getUserCod()).get();

        sessionStorage.SessionID = appSession.SessionID;
        sessionStorage.Token = appSession.Token;
        sessionStorage.PersonCod = appUser.PersonCod;
        sessionStorage.Email = appUser.Email;
        sessionStorage.Names = appUser.Email;
        sessionStorage.StoreCod = appSession.getSelectedStoreCod();
        sessionStorage.StoreList = userStoreShared.findByUserCod(appUser.UserCod).stream()
                .map(store -> storeShared.findById(store.StoreCod)).filter(java.util.Objects::nonNull).toList();
        sessionStorage.AppMenuPermissions = this.appMenuShared.findByUser(appUser.UserCod);

        ApplicationInitializationStatusDto initializationStatus =
                applicationInitializationSearchService.findForUser(appUser.UserCod);
        sessionStorage.ApplicationInitializationRequired = initializationStatus.Required;
        sessionStorage.CompanyInitializationPending = initializationStatus.CompanyPending;
        sessionStorage.StoreInitializationPending = initializationStatus.StorePending;
        sessionStorage.DefaultStoreCod = initializationStatus.DefaultStoreCod;
        return sessionStorage;
    }

    public ApplicationInitializationStatusDto findApplicationInitializationStatus() {
        return applicationInitializationSearchService.findForUser(getUserCod());
    }
}
