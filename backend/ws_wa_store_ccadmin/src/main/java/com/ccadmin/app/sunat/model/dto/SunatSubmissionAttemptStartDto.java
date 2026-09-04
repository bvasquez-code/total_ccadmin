package com.ccadmin.app.sunat.model.dto;

import com.ccadmin.app.sunat.model.entity.SunatSubmissionEntity;

public record SunatSubmissionAttemptStartDto(
        SunatSubmissionEntity submission,
        boolean sendRequired
) {
}
