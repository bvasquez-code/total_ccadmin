package com.ccadmin.app.security.model.entity.id;


import java.io.Serializable;

public class ProfileMenuID implements Serializable {

    public String ProfileCod;
    public String MenuCod;

    public ProfileMenuID()
    {

    }

    public ProfileMenuID(String profileCod, String menuCod) {
        ProfileCod = profileCod;
        MenuCod = menuCod;
    }
}
