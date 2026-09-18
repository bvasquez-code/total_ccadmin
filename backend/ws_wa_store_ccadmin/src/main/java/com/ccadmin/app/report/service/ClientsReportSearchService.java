package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.ClientsReportDto;
import com.ccadmin.app.report.model.idto.IClientsReportDto;
import com.ccadmin.app.report.repository.ClientsReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientsReportSearchService extends SessionService {
    private final SearchTService<IClientsReportDto> searchTService;

    public ClientsReportSearchService(ClientsReportRepository clientsReportRepository) {
        this.searchTService = new SearchTService<>(clientsReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<ClientsReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<ClientsReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<ClientsReportDto> toResponse(ResponsePageSearchT<IClientsReportDto> page) {
        return new ResponsePageSearchT<ClientsReportDto>().clone(
                page.resultSearch.stream().map(ClientsReportDto::new).toList(), page);
    }
}
