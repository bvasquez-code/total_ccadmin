package com.local.app.pinpad.model.dto;
public record PinpadLoginRequestDto(PinpadSignedMessageDto authorization, String applicationToken) {}
