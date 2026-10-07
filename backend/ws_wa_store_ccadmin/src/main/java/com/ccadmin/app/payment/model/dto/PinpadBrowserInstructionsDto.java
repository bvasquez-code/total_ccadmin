package com.ccadmin.app.payment.model.dto;

public record PinpadBrowserInstructionsDto(String loginUrl, String registerUrl, String statusUrl, String ackUrl,
                                           PinpadSignedMessageDto loginCommand,
                                           int waitSeconds, int pollMillis) {}
