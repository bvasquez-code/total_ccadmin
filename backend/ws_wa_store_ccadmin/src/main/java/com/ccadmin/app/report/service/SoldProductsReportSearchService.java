package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.SoldProductsReportDto;
import com.ccadmin.app.report.model.idto.ISoldProductsReportDto;
import com.ccadmin.app.report.repository.SoldProductsReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoldProductsReportSearchService extends SessionService {
    private final SearchTService<ISoldProductsReportDto> searchTService;

    public SoldProductsReportSearchService(SoldProductsReportRepository soldProductsReportRepository) {
        this.searchTService = new SearchTService<>(soldProductsReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<SoldProductsReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<SoldProductsReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<SoldProductsReportDto> toResponse(ResponsePageSearchT<ISoldProductsReportDto> page) {
        return new ResponsePageSearchT<SoldProductsReportDto>().clone(
                page.resultSearch.stream().map(SoldProductsReportDto::new).toList(), page);
    }
}
