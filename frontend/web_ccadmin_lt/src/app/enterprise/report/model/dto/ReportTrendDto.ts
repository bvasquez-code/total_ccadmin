import { FinancialReportDayDto } from './FinancialReportOverviewDto';

export interface ReportTrendPointDto {
    Date: string;
    X: number;
    SalesY: number;
    PurchasesY: number;
    ResultY: number;
    Sales: number;
    Purchases: number;
    Result: number;
}

/** Coordenadas del gráfico SVG; las cifras proceden del resumen completo del backend. */
export class ReportTrendDto {
    Points: ReportTrendPointDto[] = [];
    SalesLine = '';
    PurchasesLine = '';
    ResultLine = '';
    ZeroY = 240;
    Ticks: { Y: number; Value: number }[] = [];
    Dates: { X: number; Label: string }[] = [];

    constructor(days: FinancialReportDayDto[], from: string, to: string,
                currency: string, cumulative: boolean, metric: 'COMPARISON' | 'PROFIT') {
        const start = Date.parse(from + 'T00:00:00Z');
        const end = Date.parse(to + 'T00:00:00Z');
        if (!Number.isFinite(start) || !Number.isFinite(end) || start > end) return;
        const byDay = new Map(days.filter(day => day.CurrencyCod === currency).map(day => [day.ReportDay, day]));
        let sales = 0, purchases = 0, result = 0;
        const count = Math.round((end - start) / 86400000) + 1;
        for (let index = 0; index < count; index++) {
            const date = new Date(start + index * 86400000).toISOString().slice(0, 10);
            const day = byDay.get(date);
            sales = (cumulative ? sales : 0) + Number(day?.Sales || 0);
            purchases = (cumulative ? purchases : 0) + Number(day?.Purchases || 0);
            result = (cumulative ? result : 0) + Number(day?.KnownResult || 0);
            this.Points.push({
                Date: date, X: count === 1 ? 500 : 80 + index * 820 / (count - 1),
                Sales: sales, Purchases: purchases, Result: result,
                SalesY: 0, PurchasesY: 0, ResultY: 0
            });
        }
        const values = this.Points.flatMap(point =>
            metric === 'COMPARISON' ? [point.Sales, point.Purchases] : [point.Result]);
        const min = Math.min(0, ...values);
        const max = Math.max(0, ...values);
        const span = max - min || 1;
        const lower = min < 0 ? min - span * 0.08 : 0;
        const upper = max === min ? 1 : max + span * 0.08;
        const y = (value: number) => 240 - (value - lower) * 205 / (upper - lower);
        this.ZeroY = y(0);
        this.Points.forEach(point => {
            point.SalesY = y(point.Sales);
            point.PurchasesY = y(point.Purchases);
            point.ResultY = y(point.Result);
        });
        this.SalesLine = this.Points.map(point => `${point.X},${point.SalesY}`).join(' ');
        this.PurchasesLine = this.Points.map(point => `${point.X},${point.PurchasesY}`).join(' ');
        this.ResultLine = this.Points.map(point => `${point.X},${point.ResultY}`).join(' ');
        this.Ticks = Array.from({ length: 5 }, (_, index) => {
            const value = lower + (upper - lower) * index / 4;
            return { Value: value, Y: y(value) };
        });
        this.Dates = [...new Set([0, Math.floor((count - 1) / 2), count - 1])]
            .map(index => ({ X: this.Points[index].X, Label: this.Points[index].Date }));
    }
}
