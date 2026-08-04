package com.ccadmin.app.sunat.identity.model.dto;

import java.util.List;

public record PersonIdentityResponseDto(
        boolean found,
        String message,
        String documentTypeCode,
        String documentTypeName,
        String documentNumber,
        int resultCount,
        List<RelatedTaxpayerDto> relatedTaxpayers,
        String queryDate
) {
}
