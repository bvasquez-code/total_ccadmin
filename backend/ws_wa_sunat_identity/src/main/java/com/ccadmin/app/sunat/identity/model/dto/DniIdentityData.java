package com.ccadmin.app.sunat.identity.model.dto;

public record DniIdentityData(
        String documentNumber,
        String names,
        String paternalSurname,
        String maternalSurname
) {
}
