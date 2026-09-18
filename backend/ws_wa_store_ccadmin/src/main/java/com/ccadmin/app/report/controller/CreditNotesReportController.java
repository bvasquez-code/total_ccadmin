package com.ccadmin.app.report.controller;

import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.service.CreditNotesReportSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/report/credit-notes")
public class CreditNotesReportController {
    private final CreditNotesReportSearchService creditNotesReportSearchService;

    public CreditNotesReportController(CreditNotesReportSearchService creditNotesReportSearchService) {
        this.creditNotesReportSearchService = creditNotesReportSearchService;
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(
            @RequestParam(name = "DateFrom", defaultValue = "") String dateFrom,
            @RequestParam(name = "DateTo", defaultValue = "") String dateTo,
            @RequestParam(name = "StoreCod", defaultValue = "") String storeCod,
            @RequestParam(name = "CurrencyCod", defaultValue = "") String currencyCod,
            @RequestParam(name = "Query", defaultValue = "") String query,
            @RequestParam(name = "State", defaultValue = "") String state,
            @RequestParam(name = "Page", defaultValue = "1") int page
    ) {
        try {
            ReportFilterDto filters = new ReportFilterDto(dateFrom, dateTo, storeCod, currencyCod, query, state, page);
            return ResponseEntity.ok(new ResponseWsDto(creditNotesReportSearchService.findAll(filters)));
        } catch (IllegalArgumentException exception) {
            return new ResponseEntity<>(new ResponseWsDto(exception), HttpStatus.BAD_REQUEST);
        } catch (Exception exception) {
            ResponseWsDto response = new ResponseWsDto(exception);
            response.Data = null;
            response.Message = "No se pudo consultar el reporte. Verifique la configuración de la base de datos.";
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("export")
    public ResponseEntity<ResponseWsDto> export(
            @RequestParam(name = "DateFrom", defaultValue = "") String dateFrom,
            @RequestParam(name = "DateTo", defaultValue = "") String dateTo,
            @RequestParam(name = "StoreCod", defaultValue = "") String storeCod,
            @RequestParam(name = "CurrencyCod", defaultValue = "") String currencyCod,
            @RequestParam(name = "Query", defaultValue = "") String query,
            @RequestParam(name = "State", defaultValue = "") String state,
            @RequestParam(name = "Page", defaultValue = "1") int page
    ) {
        try {
            ReportFilterDto filters = new ReportFilterDto(dateFrom, dateTo, storeCod, currencyCod, query, state, page);
            return ResponseEntity.ok(new ResponseWsDto(creditNotesReportSearchService.export(filters)));
        } catch (IllegalArgumentException exception) {
            return new ResponseEntity<>(new ResponseWsDto(exception), HttpStatus.BAD_REQUEST);
        } catch (Exception exception) {
            ResponseWsDto response = new ResponseWsDto(exception);
            response.Data = null;
            response.Message = "No se pudo consultar el reporte. Verifique la configuración de la base de datos.";
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
