package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.dto.SessionStorageDto;
import com.ccadmin.app.security.model.entity.AppSessionEntity;
import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.security.repository.AppSessionRepository;
import com.ccadmin.app.security.repository.AppUserRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.user.shared.AppMenuShared;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityService extends SessionService {
    @Autowired
    private AppSessionRepository appSessionRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private AppMenuShared appMenuShared;

    @Transactional
    public SessionStorageDto findUserSession() {

        SessionStorageDto sessionStorage = new SessionStorageDto();
        sessionStorage.UserCod = getUserCod();

        AppSessionEntity appSession = this.appSessionRepository.findSessionEnd(sessionStorage.UserCod);
        AppUserEntity appUser = this.appUserRepository.findById(getUserCod()).get();

        this.appSessionRepository.saveHistory(getUserCod(), appSession.SessionID);
        this.appSessionRepository.cleanSession(sessionStorage.UserCod, appSession.SessionID);

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
