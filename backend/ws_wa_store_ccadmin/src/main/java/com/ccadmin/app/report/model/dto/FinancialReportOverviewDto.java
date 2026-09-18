package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IFinancialReportDayDto;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FinancialReportOverviewDto {
    public List<FinancialReportSummaryDto> Summary;
    public List<FinancialReportDayDto> Daily;

    public FinancialReportOverviewDto(List<IFinancialReportDayDto> rows) {
        Daily = rows.stream().map(FinancialReportDayDto::new).toList();
        Map<String, FinancialReportSummaryDto> totals = new TreeMap<>();
        for (FinancialReportDayDto day : Daily) {
            totals.computeIfAbsent(day.CurrencyCod, FinancialReportSummaryDto::new).add(day);
        }
        Summary = List.copyOf(totals.values());
    }
}
