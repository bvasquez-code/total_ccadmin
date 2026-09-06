package com.ccadmin.app.security.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.cash.repository.CashSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionStoreCreateService extends SessionService {
    private final CashSessionRepository cashSessionRepository;
    public SessionStoreCreateService(CashSessionRepository cashSessionRepository) {
        this.cashSessionRepository = cashSessionRepository;
    }

    @Transactional
    public void selectStore(String storeCod) {
        if (storeCod == null || userStoreShared.findByUserCod(getUserCod()).stream()
                .noneMatch(store -> storeCod.equals(store.StoreCod))) {
            throw new IllegalArgumentException("La tienda no esta asignada al usuario");
        }
        var session = appSessionRepository.findActiveBySessionId(getSessionID()).orElseThrow();
        if (!session.UserCod.equals(getUserCod())) throw new IllegalStateException("Sesion invalida");
        session.selectStore(storeCod);
        session.CashSessionID = cashSessionRepository.findOpenIdByUserAndStore(getUserCod(), storeCod).orElse(null);
        session.addSessionModify(getUserCod());
        appSessionRepository.save(session);
    }
}
