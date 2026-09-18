package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.SalesReportDto;
import com.ccadmin.app.report.model.idto.ISalesReportDto;
import com.ccadmin.app.report.repository.SalesReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesReportSearchService extends SessionService {
    private final SearchTService<ISalesReportDto> searchTService;

    public SalesReportSearchService(SalesReportRepository salesReportRepository) {
        this.searchTService = new SearchTService<>(salesReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<SalesReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<SalesReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<SalesReportDto> toResponse(ResponsePageSearchT<ISalesReportDto> page) {
        return new ResponsePageSearchT<SalesReportDto>().clone(
                page.resultSearch.stream().map(SalesReportDto::new).toList(), page);
    }
}
