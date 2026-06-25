package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.model.dto.SessionDto;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class SessionService {

    @Autowired
    protected UserStoreShared userStoreShared;
    protected SessionDto sessionDto = new SessionDto();

    public String getUserCod()
    {
        try{
            sessionDto.UserCod = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
            return sessionDto.UserCod;
        }catch (Exception ex){
            return "SISTEMA";
        }
    }

    public String getStoreCod()
    {
        sessionDto.StoreCod = this.userStoreShared.getMainStore(getUserCod());
        return sessionDto.StoreCod;
    }

}
