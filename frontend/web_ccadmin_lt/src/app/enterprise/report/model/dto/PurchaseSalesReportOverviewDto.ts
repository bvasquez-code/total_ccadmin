import { FinancialReportOverviewDto } from './FinancialReportOverviewDto';

export interface MerchandiseProfitabilityDto {
    CurrencyCod: string;
    NetSales: number;
    CostOfGoodsSold: number | null;
    StockLosses: number | null;
    SoldQuantity: number;
    LostQuantity: number;
    EstimatedResult: number | null;
    MissingCostRows: number;
}

export interface PurchaseSalesReportOverviewDto extends FinancialReportOverviewDto {
    Profitability: MerchandiseProfitabilityDto[];
}
