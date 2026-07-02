package com.ccadmin.app.shared.model.dto;

import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;

public class UrlDataDto {

    public int configOrder;
    public String urlCode;
    public String urlName;
    public String urlAddress;
    public String user;
    public String password;
    public String status;


    public UrlDataDto(BusinessConfigEntity config){
        this.configOrder = config.ConfigCorr;
        this.urlCode = config.ConfigCod;
        this.urlAddress = config.ConfigDesc;
        this.user = config.Str3Config;
        this.password = config.Str4Config;
        this.urlName = config.ConfigVal;
        this.status = config.Status;
    }


    public static UrlDataDto buildForBusinessConfig(BusinessConfigEntity config){
        return new UrlDataDto(config);
    }
}
