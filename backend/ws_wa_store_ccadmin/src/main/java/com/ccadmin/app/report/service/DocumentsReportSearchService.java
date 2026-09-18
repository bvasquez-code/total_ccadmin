package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.DocumentsReportDto;
import com.ccadmin.app.report.model.idto.IDocumentsReportDto;
import com.ccadmin.app.report.repository.DocumentsReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentsReportSearchService extends SessionService {
    private final SearchTService<IDocumentsReportDto> searchTService;

    public DocumentsReportSearchService(DocumentsReportRepository documentsReportRepository) {
        this.searchTService = new SearchTService<>(documentsReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<DocumentsReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<DocumentsReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<DocumentsReportDto> toResponse(ResponsePageSearchT<IDocumentsReportDto> page) {
        return new ResponsePageSearchT<DocumentsReportDto>().clone(
                page.resultSearch.stream().map(DocumentsReportDto::new).toList(), page);
    }
}
