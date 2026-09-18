import { ValidationHelper } from '../../../shared/helper/ValidationHelper';

export class ReportFilterDto {
    public DateFrom: string = '';
    public DateTo: string = '';
    public StoreCod: string = '';
    public CurrencyCod: string = '';
    public Query: string = '';
    public State: string = '';
    public Page: number = 1;

    constructor(storeCod: string = '', state: string = '', usesDates: boolean = true) {
        this.StoreCod = storeCod;
        this.State = state;
        if (usesDates) {
            const today = new Date();
            const yearMonth = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
            this.DateFrom = `${yearMonth}-01`;
            this.DateTo = `${yearMonth}-${String(today.getDate()).padStart(2, '0')}`;
        }
    }

    static validate(filters: ReportFilterDto, usesDates: boolean = true): void {
        ValidationHelper.validLengthString(filters.Query, 128, 'La búsqueda admite hasta 128 caracteres');
        if (usesDates) {
            ValidationHelper.validateIsNotEmpty(filters.DateFrom, 'Ingrese la fecha inicial');
            ValidationHelper.validateIsNotEmpty(filters.DateTo, 'Ingrese la fecha final');
            const from = Date.parse(filters.DateFrom);
            const to = Date.parse(filters.DateTo);
            if (!Number.isFinite(from) || !Number.isFinite(to) || from > to || (to - from) / 86400000 > 366) {
                throw new Error('El rango de fechas debe ser ordenado y no superar 366 días');
            }
        }
    }
}
