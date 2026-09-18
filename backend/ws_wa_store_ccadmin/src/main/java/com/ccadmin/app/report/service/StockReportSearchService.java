package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.StockReportDto;
import com.ccadmin.app.report.model.idto.IStockReportDto;
import com.ccadmin.app.report.repository.StockReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockReportSearchService extends SessionService {
    private final SearchTService<IStockReportDto> searchTService;

    public StockReportSearchService(StockReportRepository stockReportRepository) {
        this.searchTService = new SearchTService<>(stockReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<StockReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forStock(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<StockReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forStock(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<StockReportDto> toResponse(ResponsePageSearchT<IStockReportDto> page) {
        return new ResponsePageSearchT<StockReportDto>().clone(
                page.resultSearch.stream().map(StockReportDto::new).toList(), page);
    }
}
