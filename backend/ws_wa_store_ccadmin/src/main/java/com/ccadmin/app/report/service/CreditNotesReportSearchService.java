package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.CreditNotesReportDto;
import com.ccadmin.app.report.model.idto.ICreditNotesReportDto;
import com.ccadmin.app.report.repository.CreditNotesReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditNotesReportSearchService extends SessionService {
    private final SearchTService<ICreditNotesReportDto> searchTService;

    public CreditNotesReportSearchService(CreditNotesReportRepository creditNotesReportRepository) {
        this.searchTService = new SearchTService<>(creditNotesReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<CreditNotesReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<CreditNotesReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<CreditNotesReportDto> toResponse(ResponsePageSearchT<ICreditNotesReportDto> page) {
        return new ResponsePageSearchT<CreditNotesReportDto>().clone(
                page.resultSearch.stream().map(CreditNotesReportDto::new).toList(), page);
    }
}
