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
    public Long CashSessionID;
    public String UserCod;
    public String Token;
    public String SessionOjb;
    public Date DeleteDate;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getSelectedStoreCod() {
        if (SessionOjb == null || SessionOjb.isBlank()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(SessionOjb)
                    .path("StoreCod").asText(null);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("El contexto de sesion no es valido", exception);
        }
    }

    public void selectStore(String storeCod) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var context = SessionOjb == null || SessionOjb.isBlank()
                    ? mapper.createObjectNode()
                    : (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(SessionOjb);
            context.put("StoreCod", storeCod);
            SessionOjb = mapper.writeValueAsString(context);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("No se pudo guardar el contexto de sesion", exception);
        }
    }

    public AppSessionEntity()
    {

    }

    public AppSessionEntity(String userCod, String token) {

        this(userCod, token, null);
    }

    public AppSessionEntity(String userCod, String token, Long cashSessionId) {

        addSession(userCod,true);
        UserCod = userCod;
        CashSessionID = cashSessionId;
        Token = token;
        SessionOjb = "";
        DeleteDate = null;
    }
}
