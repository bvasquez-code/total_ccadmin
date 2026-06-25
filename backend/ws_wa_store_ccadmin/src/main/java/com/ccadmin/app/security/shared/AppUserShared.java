package com.ccadmin.app.security.shared;

import com.ccadmin.app.security.service.AppUserService;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppUserShared {

    @Autowired
    private AppUserService appUserService;

    public ResponsePageSearch findAll(String Query, int Page)
    {
        return this.appUserService.findAll(Query,Page);
    }

}
