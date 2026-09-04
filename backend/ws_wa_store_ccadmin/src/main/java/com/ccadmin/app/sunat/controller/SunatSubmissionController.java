package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionActionDto;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionSearchDto;
import com.ccadmin.app.sunat.service.SunatSubmissionCreateService;
import com.ccadmin.app.sunat.service.SunatSubmissionSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/sunatSubmission")
public class SunatSubmissionController {

    private final SunatSubmissionSearchService sunatSubmissionSearchService;
    private final SunatSubmissionCreateService sunatSubmissionCreateService;

    public SunatSubmissionController(
            SunatSubmissionSearchService sunatSubmissionSearchService,
            SunatSubmissionCreateService sunatSubmissionCreateService
    ) {
        this.sunatSubmissionSearchService = sunatSubmissionSearchService;
        this.sunatSubmissionCreateService = sunatSubmissionCreateService;
    }

    @PostMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(
            @RequestBody SunatSubmissionSearchDto request
    ) {
        return execute(() -> this.sunatSubmissionSearchService.findAll(request));
    }

    @PostMapping("retry")
    public ResponseEntity<ResponseWsDto> retry(
            @RequestBody SunatSubmissionActionDto request
    ) {
        String code = request == null ? null : request.SunatSubmissionCod;
        return execute(() -> this.sunatSubmissionCreateService.retry(code));
    }

    private ResponseEntity<ResponseWsDto> execute(Action action) {
        try {
            return ResponseEntity.ok(new ResponseWsDto(action.run()));
        } catch (Exception exception) {
            return new ResponseEntity<>(new ResponseWsDto(exception), HttpStatus.BAD_REQUEST);
        }
    }

    private interface Action {
        Object run();
    }
}
