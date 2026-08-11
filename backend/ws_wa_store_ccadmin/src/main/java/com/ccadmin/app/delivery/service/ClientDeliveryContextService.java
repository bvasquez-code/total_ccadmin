package com.ccadmin.app.delivery.service;

import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ClientDeliveryContextService {

    public ClientSessionDto getCurrentClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof ClientSessionDto clientSession)) {
            throw new IllegalStateException("La sesión del cliente no es válida");
        }
        return clientSession;
    }
}
