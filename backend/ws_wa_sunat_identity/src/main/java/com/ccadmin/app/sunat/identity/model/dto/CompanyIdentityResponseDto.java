package com.ccadmin.app.sunat.identity.model.dto;

public record CompanyIdentityResponseDto(
        boolean found,
        String message,
        CompanyIdentityDto company
) {
}
