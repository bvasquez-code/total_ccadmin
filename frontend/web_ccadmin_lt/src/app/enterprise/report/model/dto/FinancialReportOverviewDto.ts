export interface FinancialReportDayDto {
    ReportDay: string;
    CurrencyCod: string;
    Sales: number;
    Purchases: number;
    Cost: number;
    KnownResult: number;
    MissingCostRows: number;
    RowCount: number;
}

export interface FinancialReportSummaryDto {
    CurrencyCod: string;
    Sales: number;
    Purchases: number;
    Cost: number;
    KnownResult: number;
    Result: number | null;
    Margin: number | null;
    MissingCostRows: number;
    RowCount: number;
}

export interface FinancialReportOverviewDto {
    Summary: FinancialReportSummaryDto[];
    Daily: FinancialReportDayDto[];
}
