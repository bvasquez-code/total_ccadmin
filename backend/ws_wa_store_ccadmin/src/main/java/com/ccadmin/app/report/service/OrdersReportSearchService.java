package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.OrdersReportDto;
import com.ccadmin.app.report.model.idto.IOrdersReportDto;
import com.ccadmin.app.report.repository.OrdersReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdersReportSearchService extends SessionService {
    private final SearchTService<IOrdersReportDto> searchTService;

    public OrdersReportSearchService(OrdersReportRepository ordersReportRepository) {
        this.searchTService = new SearchTService<>(ordersReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<OrdersReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<OrdersReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<OrdersReportDto> toResponse(ResponsePageSearchT<IOrdersReportDto> page) {
        return new ResponsePageSearchT<OrdersReportDto>().clone(
                page.resultSearch.stream().map(OrdersReportDto::new).toList(), page);
    }
}
