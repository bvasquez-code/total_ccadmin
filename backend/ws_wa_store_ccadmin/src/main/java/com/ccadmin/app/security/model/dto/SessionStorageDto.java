package com.ccadmin.app.security.model.dto;

import com.ccadmin.app.security.model.entity.ProfileMenuEntity;
import com.ccadmin.app.user.model.entity.AppMenuEntity;

import java.util.List;

public class SessionStorageDto {

    public String Token;
    public String UserCod;
    public String PersonCod;
    public String Email;
    public String Names;
    public long SessionID;
    public String StoreCod;
    public List<AppMenuEntity> AppMenuPermissions;

}
