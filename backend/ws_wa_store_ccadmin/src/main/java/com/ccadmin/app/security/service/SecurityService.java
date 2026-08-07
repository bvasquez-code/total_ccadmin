package com.ccadmin.app.security.service;

import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.security.model.dto.SessionStorageDto;
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
    private AppUserRepository appUserRepository;
    @Autowired
    private AppMenuShared appMenuShared;
    @Autowired
    private CashSessionRepository cashSessionRepository;

    @Transactional
    public void createUserSession(String userCod, String token) {
        String storeCod = userStoreShared.getMainStore(userCod);
        Long cashSessionId = cashSessionRepository.findOpenIdByUserAndStore(userCod, storeCod)
                .orElse(null);
        appSessionRepository.save(new AppSessionEntity(userCod, token, cashSessionId));
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
        sessionStorage.StoreCod = getStoreCod();
        sessionStorage.AppMenuPermissions = this.appMenuShared.findByUser(appUser.UserCod);
        return sessionStorage;
    }
}
