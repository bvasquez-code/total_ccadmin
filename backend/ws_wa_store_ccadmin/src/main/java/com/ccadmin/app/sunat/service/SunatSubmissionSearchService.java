package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.sunat.model.constants.SunatSubmissionConstants;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionResultDto;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionSearchDto;
import com.ccadmin.app.sunat.model.entity.SunatSubmissionEntity;
import com.ccadmin.app.sunat.model.idto.ISunatSubmissionSearchDto;
import com.ccadmin.app.sunat.repository.SunatSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SunatSubmissionSearchService {

    private final SunatSubmissionRepository sunatSubmissionRepository;

    public SunatSubmissionSearchService(SunatSubmissionRepository sunatSubmissionRepository) {
        this.sunatSubmissionRepository = sunatSubmissionRepository;
    }

    public ResponsePageSearchT<SunatSubmissionResultDto> findAll(
            SunatSubmissionSearchDto request
    ) {
        SunatSubmissionSearchDto criteria = request == null
                ? new SunatSubmissionSearchDto()
                : request;
        int page = Math.max(1, criteria.Page);
        String query = clean(criteria.Query);
        String storeCod = clean(criteria.StoreCod);
        String requestType = clean(criteria.RequestType);
        String sendStatus = clean(criteria.SendStatus);
        String dateStart = cleanToNull(criteria.DateStart);
        String dateEnd = cleanToNull(criteria.DateEnd);
        int total = this.sunatSubmissionRepository.countSearch(
                query, storeCod, requestType, sendStatus, dateStart, dateEnd
        );
        List<SunatSubmissionResultDto> result = this.sunatSubmissionRepository.search(
                query,
                storeCod,
                requestType,
                sendStatus,
                dateStart,
                dateEnd,
                (page - 1) * SunatSubmissionConstants.PAGE_SIZE,
                SunatSubmissionConstants.PAGE_SIZE
        ).stream().map(SunatSubmissionResultDto::new).toList();
        ResponsePageSearchT<SunatSubmissionResultDto> response = new ResponsePageSearchT<>(
                result, page, SunatSubmissionConstants.PAGE_SIZE, total
        );
        response.StarResult = total == 0 ? 0 : response.StarResult;
        response.EndResult = Math.min(response.EndResult, total);
        return response;
    }

    public SunatSubmissionResultDto findById(String sunatSubmissionCod) {
        ISunatSubmissionSearchDto submission = this.sunatSubmissionRepository
                .findSearchById(clean(sunatSubmissionCod))
                .orElseThrow(() -> new IllegalArgumentException("No existe el envio SUNAT indicado"));
        return new SunatSubmissionResultDto(submission);
    }

    public SunatSubmissionEntity findEntityById(String sunatSubmissionCod) {
        SunatSubmissionEntity submission = this.sunatSubmissionRepository
                .findById(clean(sunatSubmissionCod))
                .orElseThrow(() -> new IllegalArgumentException("No existe el envio SUNAT indicado"));
        if (!"A".equals(submission.Status)) {
            throw new IllegalArgumentException("El envio SUNAT indicado esta inactivo");
        }
        return submission;
    }

    private String cleanToNull(String value) {
        String cleanValue = clean(value);
        return cleanValue.isBlank() ? null : cleanValue;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
