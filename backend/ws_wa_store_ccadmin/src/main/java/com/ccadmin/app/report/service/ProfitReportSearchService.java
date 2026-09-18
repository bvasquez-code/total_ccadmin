package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.FinancialReportOverviewDto;
import com.ccadmin.app.report.model.dto.ProfitReportDto;
import com.ccadmin.app.report.model.idto.IProfitReportDto;
import com.ccadmin.app.report.repository.ProfitReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProfitReportSearchService extends SessionService {
    private final ProfitReportRepository profitReportRepository;
    private final SearchTService<IProfitReportDto> searchTService;

    public ProfitReportSearchService(ProfitReportRepository profitReportRepository) {
        this.profitReportRepository = profitReportRepository;
        this.searchTService = new SearchTService<>(profitReportRepository);
    }

    public ResponsePageSearchT<ProfitReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    public FinancialReportOverviewDto overview(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return new FinancialReportOverviewDto(profitReportRepository.findDaily(search));
    }

    public ResponsePageSearchT<ProfitReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<ProfitReportDto> toResponse(ResponsePageSearchT<IProfitReportDto> page) {
        return new ResponsePageSearchT<ProfitReportDto>().clone(
                page.resultSearch.stream().map(ProfitReportDto::new).toList(), page);
    }
}
