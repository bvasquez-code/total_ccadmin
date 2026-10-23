package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IFinancialReportDayDto;
import com.ccadmin.app.report.model.idto.IMerchandiseProfitabilityDto;
import java.util.List;

public class PurchaseSalesReportOverviewDto extends FinancialReportOverviewDto {
    public List<MerchandiseProfitabilityDto> Profitability;

    public PurchaseSalesReportOverviewDto(List<IFinancialReportDayDto> daily,
                                          List<IMerchandiseProfitabilityDto> profitability) {
        super(daily);
        Profitability = profitability.stream().map(MerchandiseProfitabilityDto::new).toList();
    }
}
