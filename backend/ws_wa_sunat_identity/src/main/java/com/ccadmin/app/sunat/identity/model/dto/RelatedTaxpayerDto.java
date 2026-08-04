package com.ccadmin.app.sunat.identity.model.dto;

public record RelatedTaxpayerDto(
        String ruc,
        String legalName,
        String location,
        String status
) {
}
