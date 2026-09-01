package com.ccadmin.app.identity.model.dto;

public record CompanyIdentityResponseDto(
        boolean found,
        String message,
        CompanyIdentityDto company
) {
}
