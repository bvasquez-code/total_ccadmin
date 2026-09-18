import { ReportFilterDto } from './ReportFilterDto';

export class ReportPeriodDto {
    Mode: 'MONTH' | 'RANGE' = 'MONTH';
    Year = new Date().getFullYear();
    Month = new Date().getMonth() + 1;

    apply(filters: ReportFilterDto): void {
        if (this.Mode !== 'MONTH') return;
        if (!Number.isInteger(this.Year) || this.Year < 1000 || this.Year > 9998
                || !Number.isInteger(this.Month) || this.Month < 1 || this.Month > 12) {
            throw new Error('Seleccione un año y un mes válidos');
        }
        const month = String(this.Month).padStart(2, '0');
        const lastDay = new Date(this.Year, this.Month, 0).getDate();
        filters.DateFrom = `${this.Year}-${month}-01`;
        filters.DateTo = `${this.Year}-${month}-${lastDay}`;
    }
}
