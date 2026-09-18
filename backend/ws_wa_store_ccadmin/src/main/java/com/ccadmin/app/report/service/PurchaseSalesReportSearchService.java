package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.FinancialReportOverviewDto;
import com.ccadmin.app.report.model.dto.PurchaseSalesReportDto;
import com.ccadmin.app.report.model.idto.IPurchaseSalesReportDto;
import com.ccadmin.app.report.repository.PurchaseSalesReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PurchaseSalesReportSearchService extends SessionService {
    private final PurchaseSalesReportRepository purchaseSalesReportRepository;
    private final SearchTService<IPurchaseSalesReportDto> searchTService;

    public PurchaseSalesReportSearchService(PurchaseSalesReportRepository purchaseSalesReportRepository) {
        this.purchaseSalesReportRepository = purchaseSalesReportRepository;
        this.searchTService = new SearchTService<>(purchaseSalesReportRepository);
    }

    public ResponsePageSearchT<PurchaseSalesReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    public FinancialReportOverviewDto overview(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return new FinancialReportOverviewDto(purchaseSalesReportRepository.findDaily(search));
    }

    public ResponsePageSearchT<PurchaseSalesReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<PurchaseSalesReportDto> toResponse(ResponsePageSearchT<IPurchaseSalesReportDto> page) {
        return new ResponsePageSearchT<PurchaseSalesReportDto>().clone(
                page.resultSearch.stream().map(PurchaseSalesReportDto::new).toList(), page);
    }
}
