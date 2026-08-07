package com.ccadmin.app.shared.service;

import com.ccadmin.app.security.repository.AppSessionRepository;
import com.ccadmin.app.shared.model.dto.SessionDto;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class SessionService {

    @Autowired
    protected UserStoreShared userStoreShared;
    @Autowired
    protected AppSessionRepository appSessionRepository;

    public String getUserCod()
    {
        try{
            return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        }catch (Exception ex){
            return "SISTEMA";
        }
    }

    public String getStoreCod()
    {
        return this.userStoreShared.getMainStore(getUserCod());
    }

    public Long getSessionID() {
        return getSessionContext().SessionID;
    }

    public Long getCashSessionID() {
        return getSessionContext().CashSessionID;
    }

    protected void setCashSessionID(Long cashSessionId) {
        SessionDto sessionDto = getSessionContext();
        updateCashSessionContext(sessionDto, cashSessionId);
    }

    protected void clearCashSessionID(Long cashSessionId) {
        SessionDto sessionDto = getSessionContext();
        appSessionRepository.clearCashSessionId(cashSessionId, sessionDto.UserCod);
        updateAuthenticationContext(sessionDto, null);
    }

    private void updateCashSessionContext(SessionDto sessionDto, Long cashSessionId) {
        int updatedRows = appSessionRepository.updateCashSessionId(
                sessionDto.SessionID,
                sessionDto.UserCod,
                cashSessionId
        );
        if (updatedRows != 1) {
            throw new IllegalStateException("No se pudo actualizar la caja de la sesión autenticada");
        }
        updateAuthenticationContext(sessionDto, cashSessionId);
    }

    private void updateAuthenticationContext(SessionDto sessionDto, Long cashSessionId) {
        sessionDto.CashSessionID = cashSessionId;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AbstractAuthenticationToken authenticationToken) {
            authenticationToken.setDetails(sessionDto);
        }
    }

    private SessionDto getSessionContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof SessionDto sessionDto)) {
            throw new IllegalStateException("No existe una sesión autenticada en el contexto del backend");
        }
        return sessionDto;
    }

}
