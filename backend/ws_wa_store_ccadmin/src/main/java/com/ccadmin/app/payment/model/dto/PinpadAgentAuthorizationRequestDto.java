package com.ccadmin.app.payment.model.dto;

public record PinpadAgentAuthorizationRequestDto(PinpadSignedMessageDto authorization, String agentId, String publicKey) {}
