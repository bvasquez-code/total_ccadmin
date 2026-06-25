package com.ccadmin.app.security.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "app_session")
public class AppSessionEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long SessionID;
    public String UserCod;
    public String Token;
    public String SessionOjb;
    public Date DeleteDate;

    public AppSessionEntity()
    {

    }

    public AppSessionEntity(String userCod, String token) {

        addSession(userCod,true);
        UserCod = userCod;
        Token = token;
        SessionOjb = "";
        DeleteDate = null;
    }
}
