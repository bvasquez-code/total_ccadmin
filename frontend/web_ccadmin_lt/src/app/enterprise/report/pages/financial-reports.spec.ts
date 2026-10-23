import { CommonModule } from '@angular/common';
import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { TableComponent } from '../../shared/component/table/table.component';
import { FormatoMonedaPeruanaPipe } from '../../shared/pipe/FormatoMonedaPeruana.pipe';
import { CurrencyService } from '../../system/service/CurrencyService';
import { StoreService } from '../../store/service/store.service';
import { ResponsePageSearch } from '../../shared/model/dto/ResponsePageSearch';
import { FinancialReportOverviewDto } from '../model/dto/FinancialReportOverviewDto';
import { PurchaseSalesReportOverviewDto } from '../model/dto/PurchaseSalesReportOverviewDto';
import { ReportFilterDto } from '../model/dto/ReportFilterDto';
import { ReportPeriodDto } from '../model/dto/ReportPeriodDto';
import { ReportTrendDto } from '../model/dto/ReportTrendDto';
import { ReportService } from '../service/report.service';
import { ReportExcelService } from '../service/report-excel.service';
import { ProfitReportComponent } from './profitreport/profitreport.component';
import { PurchaseSalesReportComponent } from './purchasesalesreport/purchasesalesreport.component';

function overview(profit: boolean): FinancialReportOverviewDto & Partial<Pick<PurchaseSalesReportOverviewDto, 'Profitability'>> {
    return {
        Summary: [
            { CurrencyCod: 'PEN', Sales: 600, Purchases: profit ? 0 : 1000, Cost: profit ? 400 : 0,
              KnownResult: profit ? 200 : -400, Result: profit ? 200 : -400, Margin: profit ? 33.33 : null,
              MissingCostRows: 0, RowCount: 25 },
            { CurrencyCod: 'USD', Sales: 10, Purchases: 0, Cost: profit ? 2 : 0, KnownResult: profit ? 8 : 10,
              Result: profit ? 8 : 10, Margin: profit ? 80 : null, MissingCostRows: 0, RowCount: 1 }
        ],
        ...(!profit ? { Profitability: [
            { CurrencyCod: 'PEN', NetSales: 45, CostOfGoodsSold: 37.8, StockLosses: 4.2,
                SoldQuantity: 9, LostQuantity: 1, EstimatedResult: 3, MissingCostRows: 0 },
            { CurrencyCod: 'USD', NetSales: 10, CostOfGoodsSold: 2, StockLosses: 0,
                SoldQuantity: 1, LostQuantity: 0, EstimatedResult: 8, MissingCostRows: 0 }
        ] } : {}),
        Daily: [
            { ReportDay: '2026-09-01', CurrencyCod: 'PEN', Sales: 150, Purchases: profit ? 0 : 1000,
              Cost: profit ? 100 : 0, KnownResult: profit ? 50 : -850, MissingCostRows: 0, RowCount: 10 },
            { ReportDay: '2026-09-03', CurrencyCod: 'PEN', Sales: 450, Purchases: 0,
              Cost: profit ? 300 : 0, KnownResult: profit ? 150 : 450, MissingCostRows: 0, RowCount: 15 },
            { ReportDay: '2026-09-01', CurrencyCod: 'USD', Sales: 10, Purchases: 0,
              Cost: profit ? 2 : 0, KnownResult: profit ? 8 : 10, MissingCostRows: 0, RowCount: 1 }
        ]
    };
}

for (const report of [
    { component: ProfitReportComponent as Type<any>, name: 'Profit', lines: 1 },
    { component: PurchaseSalesReportComponent as Type<any>, name: 'PurchaseSales', lines: 2 }
]) {
    describe(report.component.name, () => {
        let service: jasmine.SpyObj<any>;
        let excel: jasmine.SpyObj<any>;
        let toastr: jasmine.SpyObj<any>;
        let financialOverview: ReturnType<typeof overview>;

        beforeEach(async () => {
            financialOverview = overview(report.name === 'Profit');
            service = jasmine.createSpyObj('ReportService', ['findDataForm', 'find' + report.name, 'overview' + report.name, 'export' + report.name]);
            service.findDataForm.and.resolveTo({ ErrorStatus: false, DataAdditional: [
                { Name: 'storeList', Data: [{ StoreCod: 'T001', Name: 'Local 1' }] },
                { Name: 'currencyList', Data: [{ CurrencyCod: 'PEN', CurrencyName: 'Soles' }] }
            ] });
            const page = Object.assign(new ResponsePageSearch(), {
                Page: 1, TotalPages: 3, TotalResult: 26, StarResult: 1, EndResult: 10,
                resultSearch: [{ OperationCod: 'S1', SourceType: 'SALE', TypeOperation: 'R', Quantity: 1, Amount: 150, Sales: 150,
                    Purchases: 0, Balance: 150, Cost: 100, Profit: 50, CurrencyCod: 'PEN', CostStatus: 'COMPLETE' }]
            });
            service['find' + report.name].and.resolveTo({ ErrorStatus: false, Data: page });
            service['overview' + report.name].and.resolveTo({ ErrorStatus: false, Data: financialOverview });
            service['export' + report.name].and.resolveTo({ ErrorStatus: false, Data: page });
            excel = jasmine.createSpyObj('ReportExcelService', ['download']);
            toastr = jasmine.createSpyObj('ToastrService', ['error']);
            await TestBed.configureTestingModule({
                imports: [CommonModule, FormsModule],
                declarations: [report.component, TableComponent, FormatoMonedaPeruanaPipe],
                providers: [
                    { provide: ReportService, useValue: service },
                    { provide: ReportExcelService, useValue: excel },
                    { provide: ToastrService, useValue: toastr },
                    { provide: DataSesionService, useValue: {
                        getSessionStorageDto: () => ({ StoreCod: 'T001' })
                    } },
                    { provide: StoreService, useValue: {
                        FindAllList: async () => ({ ErrorStatus: false, Data: [{ StoreCod: 'T001', Name: 'Local 1', Status: 'A' }] })
                    } },
                    { provide: CurrencyService, useValue: {
                        findActives: async () => ({ ErrorStatus: false, Data: [{ CurrencyCod: 'PEN', CurrencyName: 'Soles' }] })
                    } }
                ]
            }).compileComponents();
        });

        async function render() {
            const fixture = TestBed.createComponent(report.component);
            fixture.detectChanges();
            await fixture.whenStable();
            fixture.componentInstance.period.Year = 2026;
            fixture.componentInstance.period.Month = 9;
            await fixture.componentInstance.findAll(1, '');
            fixture.detectChanges();
            await fixture.whenStable();
            return fixture;
        }

        it('renders the shared table, full-period summary and the correct number of chart lines', async () => {
            const fixture = await render();
            const component = fixture.componentInstance;
            const table = fixture.debugElement.query(By.directive(TableComponent)).componentInstance;
            expect(table.actionTableService).toBe(component);
            expect(component.summary.Sales).toBe(600);
            expect(component.responsePageSearch.resultSearch.length).toBe(1);
            expect(fixture.nativeElement.querySelectorAll('svg polyline').length).toBe(report.lines);
            expect(fixture.nativeElement.textContent).toContain('600.00');
            expect(component.trend.Points.length).toBe(30);
            expect(component.trend.Points[29].Result).toBe(report.name === 'Profit' ? 200 : -400);
        });

        it('keeps summaries separate when switching currency without reloading the detail', async () => {
            const fixture = await render();
            const component = fixture.componentInstance;
            const calls = service['find' + report.name].calls.count();
            component.summaryCurrency = 'USD';
            component.updateChart();
            fixture.detectChanges();
            expect(component.summary.Sales).toBe(10);
            expect(component.trend.Points[29].Sales).toBe(10);
            expect(service['find' + report.name].calls.count()).toBe(calls);
        });

        it('keeps the applied month when paginating after editing the form', async () => {
            const fixture = await render();
            const component = fixture.componentInstance;
            component.period.Month = 10;
            component.filters.StoreCod = 'T002';
            component.filter(2);
            await fixture.whenStable();
            const filters = service['find' + report.name].calls.mostRecent().args[0];
            expect(filters.DateFrom).toBe('2026-09-01');
            expect(filters.DateTo).toBe('2026-09-30');
            expect(filters.StoreCod).toBe('T001');
            expect(filters.Page).toBe(2);
            expect(service['overview' + report.name].calls.mostRecent().args[0]).toBe(filters);
        });

        it('supports leap-year months and preserves an explicit date range', async () => {
            const fixture = await render();
            const component = fixture.componentInstance;
            component.period.Year = 2024;
            component.period.Month = 2;
            await component.findAll(1, '');
            expect(component.appliedFilters.DateTo).toBe('2024-02-29');
            component.period.Mode = 'RANGE';
            component.filters.DateFrom = '2026-08-15';
            component.filters.DateTo = '2026-09-05';
            await component.findAll(1, '');
            expect(component.appliedFilters.DateFrom).toBe('2026-08-15');
            expect(component.appliedFilters.DateTo).toBe('2026-09-05');
        });

        it('exports the full overview alongside detail and handles summary failures atomically', async () => {
            const fixture = await render();
            const component = fixture.componentInstance;
            await component.exportExcel();
            expect(excel.download.calls.mostRecent().args[3]).toBe(financialOverview);
            service['overview' + report.name].and.resolveTo({ ErrorStatus: true, Message: 'Resumen no disponible' });
            await component.findAll(1, '');
            expect(component.overviewData).toBeUndefined();
            expect(component.hasSearched).toBeFalse();
            expect(component.responsePageSearch.resultSearch).toEqual([]);
            expect(toastr.error).toHaveBeenCalledWith('Resumen no disponible');
        });

        if (report.name === 'Profit') {
            it('labels incomplete cost coverage as partial and leaves the margin unavailable', async () => {
                const fixture = await render();
                financialOverview.Summary[0].MissingCostRows = 2;
                financialOverview.Summary[0].Result = null;
                financialOverview.Summary[0].Margin = null;
                await fixture.componentInstance.findAll(1, '');
                fixture.detectChanges();
                expect(fixture.nativeElement.textContent).toContain('Utilidad verificada (parcial)');
                expect(fixture.nativeElement.textContent).toContain('2 siguen pendientes');
            });
        } else {
            it('renders the four profitability indicators and switches them with the summary currency', async () => {
                const fixture = await render();
                const section: HTMLElement = fixture.nativeElement.querySelector('[aria-labelledby="profitability-title"]');
                expect(section.textContent).toContain('Ventas netas');
                expect(section.textContent).toContain('Costo de mercadería vendida');
                expect(section.textContent).toContain('Pérdidas por bajas');
                expect(section.textContent).toContain('Resultado comercial estimado');
                expect(section.textContent).toContain('45.00');
                expect(section.textContent).toContain('37.80');
                expect(section.textContent).toContain('4.20');
                expect(section.textContent).toContain('+3.00');
                fixture.componentInstance.summaryCurrency = 'USD';
                fixture.detectChanges();
                expect(section.textContent).toContain('+8.00');
                expect(section.textContent).not.toContain('45.00');
            });

            it('retains the applied product filter for pagination and export until a new query is submitted', async () => {
                const fixture = await render();
                const component = fixture.componentInstance;
                const input: HTMLInputElement = fixture.nativeElement.querySelector('#report-product');
                input.value = '  atún  ';
                input.dispatchEvent(new Event('input'));
                await fixture.whenStable();
                await component.findAll(1, '');
                expect(service.findPurchaseSales.calls.mostRecent().args[0].ProductQuery).toBe('atún');
                expect(service.overviewPurchaseSales.calls.mostRecent().args[0].ProductQuery).toBe('atún');
                component.filters.ProductQuery = 'otro producto';
                const paginate = spyOn(component, 'findAll').and.callThrough();
                component.filter(2);
                await paginate.calls.mostRecent().returnValue;
                expect(service.findPurchaseSales.calls.mostRecent().args[0].ProductQuery).toBe('atún');
                await component.exportExcel();
                expect(service.exportPurchaseSales.calls.mostRecent().args[0].ProductQuery).toBe('atún');
                expect(excel.download.calls.mostRecent().args[1].ProductQuery).toBe('atún');
                expect(excel.download.calls.mostRecent().args[3].Profitability).toBe(financialOverview.Profitability);
                component.resetFilters();
                expect(component.filters.ProductQuery).toBe('');
            });

            it('shows pending costs without presenting a numeric estimated result', async () => {
                const fixture = await render();
                const totals = financialOverview.Profitability![0];
                totals.MissingCostRows = 1;
                totals.CostOfGoodsSold = null;
                totals.StockLosses = null;
                totals.EstimatedResult = null;
                fixture.detectChanges();
                const section: HTMLElement = fixture.nativeElement.querySelector('[aria-labelledby="profitability-title"]');
                expect(section.textContent).toContain('Pendiente');
                expect(section.textContent).toContain('moneda de costo incompatible');
                expect(section.textContent).not.toContain('+3.00');
            });

            it('allows all stores and exposes transfers and both stock movement types', async () => {
                const fixture = await render();
                const component = fixture.componentInstance;
                const storeOptions = Array.from(fixture.nativeElement.querySelectorAll('#report-store option')) as HTMLOptionElement[];
                expect(storeOptions.find(option => option.value === '')?.textContent).toContain('Todas las tiendas');
                expect(fixture.nativeElement.textContent).toContain('T001 - Local 1');
                const types = Array.from(fixture.nativeElement.querySelectorAll('#report-state option')) as HTMLOptionElement[];
                expect(types.map(option => option.value)).toEqual(['', 'PURCHASE', 'SALE', 'CREDIT_NOTE', 'TRANSFER', 'STOCK_ENTRY', 'STOCK_EXIT']);
                expect(fixture.nativeElement.textContent).toContain('Salidas valorizadas (+)');
                expect(fixture.nativeElement.textContent).toContain('Entradas valorizadas (−)');
                component.filters.StoreCod = '';
                await component.findAll(1, '');
                expect(service.findPurchaseSales.calls.mostRecent().args[0].StoreCod).toBe('');
                expect(component.appliedStoreName).toBe('Todas las tiendas');
                expect(toastr.error).not.toHaveBeenCalled();
            });

            it('preserves the selected store and operation type in queries, pagination and export', async () => {
                const fixture = await render();
                const component = fixture.componentInstance;
                component.filters.State = 'TRANSFER';
                await component.findAll(1, 'TR1');
                expect(component.appliedFilters.State).toBe('TRANSFER');
                expect(component.appliedFilters.StoreCod).toBe('T001');
                component.filters.StoreCod = 'T002';
                component.filters.State = 'STOCK_ENTRY';
                await component.exportExcel();
                expect(service.exportPurchaseSales.calls.mostRecent().args[0].StoreCod).toBe('T001');
                expect(service.exportPurchaseSales.calls.mostRecent().args[0].State).toBe('TRANSFER');
                expect(component.appliedStoreName).toBe('T001 - Local 1');
            });
        }
    });
}

describe('ReportTrendDto', () => {
    it('includes days without operations and allows negative cumulative balances', () => {
        const chart = new ReportTrendDto(overview(false).Daily, '2026-09-01', '2026-09-03', 'PEN', true, 'COMPARISON');
        expect(chart.Points[1].Sales).toBe(150);
        expect(chart.Points[1].Purchases).toBe(1000);
        expect(chart.Points[2].Result).toBe(-400);
        const daily = new ReportTrendDto(overview(false).Daily, '2026-09-01', '2026-09-03', 'PEN', false, 'COMPARISON');
        expect(daily.Points[1].Sales).toBe(0);
        expect(daily.Points[1].Purchases).toBe(0);
    });

    it('produces finite coordinates for one day without operations', () => {
        const chart = new ReportTrendDto([], '2026-09-01', '2026-09-01', 'PEN', true, 'PROFIT');
        expect(chart.Points.length).toBe(1);
        expect(chart.ResultLine).not.toContain('NaN');
        expect(chart.ResultLine).not.toContain('Infinity');
    });
});

describe('ReportPeriodDto', () => {
    it('rejects invalid years and months before forming a query', () => {
        const period = new ReportPeriodDto();
        period.Month = 13;
        expect(() => period.apply(new ReportFilterDto())).toThrowError();
        period.Month = 1;
        period.Year = 9999;
        expect(() => period.apply(new ReportFilterDto())).toThrowError();
    });
});
