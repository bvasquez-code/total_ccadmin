package com.ccadmin.app.sunat.identity.model.dto;

import java.util.List;

public record CompanyIdentityDto(
        String ruc,
        String legalName,
        String taxpayerType,
        String tradeName,
        String registrationDate,
        String businessStartDate,
        String taxpayerStatus,
        String taxpayerCondition,
        String fiscalAddress,
        String receiptIssuanceSystem,
        String foreignTradeActivity,
        String accountingSystem,
        List<String> economicActivities,
        List<String> authorizedPaymentReceipts,
        List<String> electronicIssuanceSystems,
        String electronicIssuerSince,
        List<String> electronicReceipts,
        String pleMemberSince,
        List<String> registries,
        String queryDate
) {
}
