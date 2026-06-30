package com.ccadmin.app.shared.service;

import org.springframework.security.core.context.SecurityContextHolder;

public abstract class SessionService {

    public String getUserCod() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return principal == null ? "SISTEMA" : principal.toString();
        } catch (Exception ex) {
            return "SISTEMA";
        }
    }
}
