package com.ccadmin.app.report.controller;

import com.ccadmin.app.report.service.ReportFormSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/report")
public class ReportFormController {
    private final ReportFormSearchService reportFormSearchService;

    public ReportFormController(ReportFormSearchService reportFormSearchService) {
        this.reportFormSearchService = reportFormSearchService;
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm() {
        try {
            return ResponseEntity.ok(reportFormSearchService.findDataForm());
        } catch (Exception exception) {
            return new ResponseEntity<>(new ResponseWsDto(exception), HttpStatus.BAD_REQUEST);
        }
    }
}
