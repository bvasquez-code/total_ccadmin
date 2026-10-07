package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.dto.PinpadAgentAuthorizationRequestDto;
import com.ccadmin.app.shared.service.SessionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Set;

@Service
public class PinpadAuthorizationCreateService extends SessionService {
    private final PinpadSignatureService pinpadSignatureService;
    private final PinpadTrustCreateService pinpadTrustCreateService;
    private final PinpadConfigurationSearchService pinpadConfigurationSearchService;
    public PinpadAuthorizationCreateService(PinpadSignatureService pinpadSignatureService,
            PinpadTrustCreateService pinpadTrustCreateService, PinpadConfigurationSearchService pinpadConfigurationSearchService) {
        this.pinpadSignatureService = pinpadSignatureService;
        this.pinpadTrustCreateService = pinpadTrustCreateService;
        this.pinpadConfigurationSearchService = pinpadConfigurationSearchService;
    }
    public JsonNode authorize(PinpadAgentAuthorizationRequestDto request) {
        if (request == null || request.publicKey() == null || request.publicKey().length() > 4096) {
            throw new IllegalArgumentException("Solicitud de login pinpad invalida");
        }
        JsonNode command = pinpadSignatureService.verifyCommand(request.authorization(), pinpadTrustCreateService.commandSigningKey());
        Long cashSessionId = getCashSessionID();
        String storeCod = getStoreCod();
        boolean sameCashSession = cashSessionId == null ? command.path("cashSessionId").isNull()
                : command.path("cashSessionId").isIntegralNumber() && command.path("cashSessionId").asLong() == cashSessionId;
        if (!sameCashSession || !storeCod.equals(command.path("storeCod").asText())
                || !getUserCod().equals(command.path("request").path("cashier").asText())
                || command.path("expiresAt").asLong(0) <= Instant.now().getEpochSecond()
                || !Set.of("REGISTER", "ACK").contains(command.path("operation").asText())) {
            throw new IllegalArgumentException("El login pinpad no corresponde a la sesion autenticada o vencio");
        }
        var connection = pinpadConfigurationSearchService.connection(getUserCod(), cashSessionId, storeCod);
        if (!connection.agentId().equals(request.agentId()) || !connection.agentId().equals(command.path("agentId").asText())) {
            throw new IllegalArgumentException("El terminal no corresponde a esta caja");
        }
        pinpadTrustCreateService.registerAgent(connection.agentId(), request.publicKey());
        return command;
    }
}
