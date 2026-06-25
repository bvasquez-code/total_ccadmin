package com.ccadmin.app.security.model.entity.id;

public class UserProfileID {

    public String UserCod;
    public String ProfileCod;

    public UserProfileID() {
    }

    public UserProfileID(String userCod, String profileCod) {
        UserCod = userCod;
        ProfileCod = profileCod;
    }
}
