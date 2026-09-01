package com.ccadmin.app.identity.model.dto;

public record RelatedTaxpayerDto(
        String ruc,
        String legalName,
        String location,
        String status
) {
}
