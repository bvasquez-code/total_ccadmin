package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.dto.PaymentMethodsReportDto;
import com.ccadmin.app.report.model.idto.IPaymentMethodsReportDto;
import com.ccadmin.app.report.repository.PaymentMethodsReportRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentMethodsReportSearchService extends SessionService {
    private final SearchTService<IPaymentMethodsReportDto> searchTService;

    public PaymentMethodsReportSearchService(PaymentMethodsReportRepository paymentMethodsReportRepository) {
        this.searchTService = new SearchTService<>(paymentMethodsReportRepository);
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<PaymentMethodsReportDto> findAll(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAll(search, ReportConstants.PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public ResponsePageSearchT<PaymentMethodsReportDto> export(ReportFilterDto filters) {
        ReportSearchDto search = ReportSearchDto.forPeriod(filters, getUserCod());
        return toResponse(searchTService.findAllForExport(search, ReportConstants.EXPORT_LIMIT));
    }

    private ResponsePageSearchT<PaymentMethodsReportDto> toResponse(ResponsePageSearchT<IPaymentMethodsReportDto> page) {
        return new ResponsePageSearchT<PaymentMethodsReportDto>().clone(
                page.resultSearch.stream().map(PaymentMethodsReportDto::new).toList(), page);
    }
}
