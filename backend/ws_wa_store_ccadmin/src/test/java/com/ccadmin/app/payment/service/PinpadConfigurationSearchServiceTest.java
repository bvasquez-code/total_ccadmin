package com.ccadmin.app.payment.service;

import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.payment.config.PinpadBrowserProperties;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PinpadConfigurationSearchServiceTest {
    final BusinessConfigSearchService configurations = mock(BusinessConfigSearchService.class);
    final CashSessionRepository sessions = mock(CashSessionRepository.class);
    final PinpadBrowserProperties properties = new PinpadBrowserProperties();
    final PinpadConfigurationSearchService service = new PinpadConfigurationSearchService(configurations, sessions, properties);

    @Test
    void selectsDefaultInstallationWithoutAnyCashSession() {
        assertEquals("CAJA01", service.connection("USER1", null, "T001").agentId());
        verifyNoInteractions(sessions);
        assertThrows(IllegalStateException.class, () -> service.connection("USER1", null, "OTHER"));
    }

    @Test
    void doesNotGuessTerminalWhenStoreHasSeveralAgentsWithoutCashSession() {
        registerAgent("PC1", "R1"); registerAgent("PC2", "R2");
        assertThrows(IllegalStateException.class, () -> service.connection("USER1", null, "S1"));
        verifyNoInteractions(sessions);
    }

    @Test
    void refusesCashSessionFromAnotherStore() {
        registerSession(1L, "R1"); registerAgent("PC1", "R1");
        assertThrows(IllegalArgumentException.class, () -> service.connection("USER1", 1L, "OTHER"));
    }

    @Test
    void selectsDifferentAgentForEachAuthenticatedCashRegister() {
        registerSession(1L, "R1"); registerSession(2L, "R2");
        registerAgent("PC1", "R1"); registerAgent("PC2", "R2");
        registerUrls("http://127.0.0.1:8094/browser-pinpad");
        assertEquals("PC1", service.connection("USER1", 1L, "S1").agentId());
        assertEquals("PC2", service.connection("USER1", 2L, "S1").agentId());
        assertEquals(service.connection("USER1", 1L, "S1").registerUrl(), service.connection("USER1", 2L, "S1").registerUrl());
    }

    @Test
    void rejectsUnknownAmbiguousAndInactiveCashRegisterAndDifferentOwner() {
        var session = registerSession(1L, "R1");
        assertThrows(IllegalStateException.class, () -> service.connection("USER1", 1L, "S1"));
        registerAgent("PC1", "R1"); registerAgent("PC2", "R1");
        assertThrows(IllegalStateException.class, () -> service.connection("USER1", 1L, "S1"));
        assertThrows(IllegalArgumentException.class, () -> service.connection("USER2", 1L, "S1"));
        session.Status = "I";
        assertThrows(IllegalArgumentException.class, () -> service.connection("USER1", 1L, "S1"));
    }

    @Test
    void selectsAgentForClosedCashSessionWithoutRequiringOpening() {
        var session = registerSession(1L, "R1");
        session.SessionStatus = 'C'; session.IsOpen = 0;
        registerAgent("PC1", "R1");
        registerUrls("http://127.0.0.1:8094/browser-pinpad");
        assertEquals("PC1", service.connection("USER1", 1L, "S1").agentId());
    }

    @Test
    void usesLocalDefaultsWhenUrlConfigurationIsNotInstalled() {
        registerSession(1L, "R1"); registerAgent("PC1", "R1");
        var connection = service.connection("USER1", 1L, "S1");
        assertEquals("http://127.0.0.1:8094/pinpad/payment/register", connection.registerUrl());
        assertEquals("http://127.0.0.1:8094/pinpad/payment/status", connection.statusUrl());
        assertEquals("http://127.0.0.1:8094/pinpad/payment/ack", connection.ackUrl());
        assertEquals(130, connection.waitSeconds());
        assertEquals(1000, connection.pollMillis());
    }

    @Test
    void preservesCustomUrlsAndExplicitlyInactiveConfiguration() {
        registerSession(1L, "R1"); registerAgent("PC1", "R1");
        registerUrls("http://127.0.0.1:9094/browser-pinpad");
        assertEquals("http://127.0.0.1:9094/browser-pinpad", service.connection("USER1", 1L, "S1").registerUrl());
        var disabled = new BusinessConfigEntity(); disabled.Status = "I";
        when(configurations.findByConfigCod(PinpadConfigurationSearchService.CONFIG_GROUP,
                PinpadConfigurationSearchService.REGISTER_CONFIG)).thenReturn(disabled);
        assertThrows(IllegalStateException.class, () -> service.connection("USER1", 1L, "S1"));
    }

    @Test
    void rejectsServerUrlsAndBlockedPort() {
        registerSession(1L, "R1");
        var agent = registerAgent("PC1", "R1");
        registerUrls("http://cloud.example/browser-pinpad");
        assertThrows(IllegalStateException.class, () -> service.connection("USER1", 1L, "S1"));
        registerUrls("http://127.0.0.1:6669/browser-pinpad");
        assertThrows(IllegalStateException.class, () -> service.connection("USER1", 1L, "S1"));
    }

    CashSessionEntity registerSession(long id, String register) {
        var session = new CashSessionEntity();
        session.StoreCod = "S1"; session.RegisterCod = register; session.UserCod = "USER1";
        when(sessions.findByCashSessionId(id)).thenReturn(Optional.of(session));
        return session;
    }
    PinpadBrowserProperties.Agent registerAgent(String id, String register) {
        var agent = new PinpadBrowserProperties.Agent();
        agent.setStoreCod("S1"); agent.setRegisterCod(register);
        properties.getAgents().put(id, agent);
        return agent;
    }
    void registerUrls(String url) {
        for (String code : new String[]{PinpadConfigurationSearchService.REGISTER_CONFIG,
                PinpadConfigurationSearchService.STATUS_CONFIG, PinpadConfigurationSearchService.ACK_CONFIG,
                PinpadConfigurationSearchService.LOGIN_CONFIG}) {
            var config = new BusinessConfigEntity(); config.ConfigVal = url;
            when(configurations.findByConfigCod(PinpadConfigurationSearchService.CONFIG_GROUP, code)).thenReturn(config);
        }
    }
}
