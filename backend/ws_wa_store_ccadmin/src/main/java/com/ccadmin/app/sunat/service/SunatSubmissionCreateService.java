package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionResultDto;
import org.springframework.stereotype.Service;

@Service
public class SunatSubmissionCreateService extends SessionService {

    private final SaleSunatClientService saleSunatClientService;
    private final SunatSubmissionSearchService sunatSubmissionSearchService;

    public SunatSubmissionCreateService(
            SaleSunatClientService saleSunatClientService,
            SunatSubmissionSearchService sunatSubmissionSearchService
    ) {
        this.saleSunatClientService = saleSunatClientService;
        this.sunatSubmissionSearchService = sunatSubmissionSearchService;
    }

    public SunatSubmissionResultDto retry(String sunatSubmissionCod) {
        this.saleSunatClientService.retrySubmission(sunatSubmissionCod, this.getUserCod());
        return this.sunatSubmissionSearchService.findById(sunatSubmissionCod);
    }
}
