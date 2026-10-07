package com.local.app.pinpad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Service
public class PinpadBrowserCreateService {
    private final PinpadPaymentService pinpadPaymentService;
    private final PinpadAgentProperties pinpadAgentProperties;
    private final ObjectMapper objectMapper;
    private final PinpadAccessCreateService pinpadAccessCreateService;
    private final PinpadIdentityCreateService pinpadIdentityCreateService;

    public PinpadBrowserCreateService(PinpadPaymentService pinpadPaymentService,
            PinpadAgentProperties pinpadAgentProperties, ObjectMapper objectMapper,
            PinpadAccessCreateService pinpadAccessCreateService, PinpadIdentityCreateService pinpadIdentityCreateService) {
        this.pinpadPaymentService = pinpadPaymentService;
        this.pinpadAgentProperties = pinpadAgentProperties;
        this.objectMapper = objectMapper;
        this.pinpadAccessCreateService = pinpadAccessCreateService;
        this.pinpadIdentityCreateService = pinpadIdentityCreateService;
    }

    public PinpadSignedMessageDto execute(String authorization, String operation) {
        JsonNode command = pinpadAccessCreateService.command(authorization);
        String granted = command.path("operation").asText();
        if (!(granted.equals(operation) || ("REGISTER".equals(granted) && "STATUS".equals(operation)))) throw unauthorized();
        ((com.fasterxml.jackson.databind.node.ObjectNode) command).put("operation", operation);
        PinpadPaymentRegisterDto request;
        try {
            request = objectMapper.treeToValue(command.path("request"), PinpadPaymentRegisterDto.class);
        } catch (Exception exception) { throw unauthorized(); }
        JsonNode cashSession = command.path("cashSessionId");
        if (request == null || request.getPaymentId() == null
                || !(cashSession.isNull() || (cashSession.isIntegralNumber() && cashSession.asLong() > 0))) throw unauthorized();
        String referencePrefix = cashSession.isNull() ? "agent-" + pinpadAgentProperties.getAgentId()
                : "cash-session-" + cashSession.asLong();
        if (!Objects.equals(request.getExternalReference(), referencePrefix + "-" + request.getPaymentId())) throw unauthorized();

        PinpadPaymentDetailDto detail;
        switch (command.path("operation").asText()) {
            case "REGISTER" -> detail = pinpadPaymentService.registerPayment(request);
            case "STATUS" -> detail = pinpadPaymentService.findPayment(request.getPaymentId());
            case "ACK" -> {
                detail = pinpadPaymentService.findPayment(request.getPaymentId());
                assertSameRequest(detail, request);
                if (!command.path("centralPaymentCod").asText().matches("[1-9][0-9]*")) throw unauthorized();
                PinpadPaymentAckDto ack = new PinpadPaymentAckDto();
                ack.setCentralSaved(true);
                ack.setCentralPaymentCod(command.path("centralPaymentCod").asText());
                detail = pinpadPaymentService.ackPayment(request.getPaymentId(), ack);
            }
            default -> throw unauthorized();
        }
        assertSameRequest(detail, request);
        return signResult(Map.of("agentId", pinpadAgentProperties.getAgentId(), "command", command,
                "result", PinpadPaymentStatusDto.fromDetail(detail)));
    }

    private void assertSameRequest(PinpadPaymentDetailDto detail, PinpadPaymentRegisterDto request) {
        if (!Objects.equals(detail.getAmountCents(), request.getAmountCents())
                || !Objects.equals(detail.getCurrency(), request.getCurrency())
                || detail.getPaymentMethod() != request.getPaymentMethod()
                || !Objects.equals(detail.getCashier(), request.getCashier())
                || !Objects.equals(detail.getInternalPaymentCode(), request.getInternalPaymentCode())
                || !Objects.equals(detail.getExternalReference(), request.getExternalReference())) throw unauthorized();
    }

    private PinpadSignedMessageDto signResult(Object data) {
        try {
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(data));
            return new PinpadSignedMessageDto(payload, pinpadIdentityCreateService.signResult(payload));
        } catch (Exception exception) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST,
                    "No se pudo firmar el resultado; consultar la misma referencia");
        }
    }

    private PinpadPaymentException unauthorized() {
        return new PinpadPaymentException(PinpadErrorCode.UNAUTHORIZED_AGENT,
                "Autorizacion pinpad invalida, vencida o dirigida a otra PC", HttpStatus.UNAUTHORIZED);
    }
}
