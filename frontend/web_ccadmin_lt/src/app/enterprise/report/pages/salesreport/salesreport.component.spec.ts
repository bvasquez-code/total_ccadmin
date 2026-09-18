import { CommonModule } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { TableComponent } from '../../../shared/component/table/table.component';
import { DataTablaGeneticDto } from '../../../shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { FormatoMonedaPeruanaPipe } from '../../../shared/pipe/FormatoMonedaPeruana.pipe';
import { CurrencyService } from '../../../system/service/CurrencyService';
import { StoreService } from '../../../store/service/store.service';
import { ReportFilterDto } from '../../model/dto/ReportFilterDto';
import { ReportRowDto } from '../../model/dto/ReportRowDto';
import { ReportExcelService } from '../../service/report-excel.service';
import { ReportService } from '../../service/report.service';
import { ProfitReportComponent } from '../profitreport/profitreport.component';
import { StockReportComponent } from '../stockreport/stockreport.component';
import { SalesReportComponent } from './salesreport.component';

function page(saleCod = 'S001'): ResponsePageSearch<ReportRowDto> {
    return Object.assign(new ResponsePageSearch<ReportRowDto>(), {
        resultSearch: [{ SaleCod: saleCod, Amount: 123.45, SaleStatus: 'C', CurrencyCod: 'USD' }],
        TotalResult: 21, TotalPages: 3, StarResult: 1, EndResult: 10, Page: 1
    });
}

function dependencies() {
    const reportService = jasmine.createSpyObj('ReportService', ['findSales', 'exportSales']);
    reportService.findSales.and.resolveTo({ ErrorStatus: false, Data: page() });
    const reportExcelService = jasmine.createSpyObj('ReportExcelService', ['download']);
    const dataSesionService = {
        getSessionStorageDto: () => ({
            StoreCod: 'T001'
        })
    };
    const currencyService = jasmine.createSpyObj('CurrencyService', ['findActives']);
    currencyService.findActives.and.resolveTo({ ErrorStatus: false, Data: [{ CurrencyCod: 'USD', CurrencyName: 'Dólares' }] });
    const toastrService = jasmine.createSpyObj('ToastrService', ['error']);
    const storeService = jasmine.createSpyObj('StoreService', ['FindAllList']);
    storeService.FindAllList.and.resolveTo({ ErrorStatus: false, Data: [
        { StoreCod: 'T001', Name: 'Local 1', Status: 'A' }, { StoreCod: 'T002', Name: 'Local 2', Status: 'A' }
    ] });
    return { reportService, reportExcelService, dataSesionService, currencyService, toastrService, storeService };
}

describe('SalesReportComponent', () => {
    let component: SalesReportComponent;
    let services: ReturnType<typeof dependencies>;

    beforeEach(() => {
        services = dependencies();
        component = new SalesReportComponent(
            services.reportService, services.reportExcelService, services.dataSesionService as any,
            services.currencyService, services.toastrService, services.storeService
        );
        component.resetFilters();
    });

    it('ignores a response arriving after another sales request', async () => {
        let resolveFirst!: (response: any) => void;
        const first = new Promise(resolve => resolveFirst = resolve);
        const secondData = page('S002');
        services.reportService.findSales.and.returnValues(first, Promise.resolve({ ErrorStatus: false, Data: secondData }));
        const firstRequest = component.findAll(1, 'primera');
        await component.findAll(1, 'segunda');
        resolveFirst({ ErrorStatus: false, Data: page('S001') });
        await firstRequest;
        expect(component.responsePageSearch).toBe(secondData);
        expect(component.dataTablaGenetic.DataTable.data).toBe(secondData);
    });

    it('does not consume failed backend responses', async () => {
        services.reportService.findSales.and.resolveTo({ ErrorStatus: true, Message: 'Consulta fallida', Data: {} });
        await component.findAll(1, '');
        expect(component.responsePageSearch.resultSearch).toEqual([]);
        expect(component.appliedFilters).toBeUndefined();
        expect(services.toastrService.error).toHaveBeenCalledWith('Consulta fallida');
    });

    it('rejects reversed dates before requesting data', async () => {
        component.filters.DateFrom = '2026-09-30';
        component.filters.DateTo = '2026-09-01';
        await component.findAll(1, '');
        expect(services.reportService.findSales).not.toHaveBeenCalled();
        expect(services.toastrService.error).toHaveBeenCalled();
    });

    it('keeps applied filters when navigating forward and back to the first page', async () => {
        await component.findAll(1, 'aplicado');
        component.filters.Query = 'sin aplicar';
        component.filters.StoreCod = 'T002';

        component.filter(2);
        await Promise.resolve();
        expect(services.reportService.findSales).toHaveBeenCalledWith(
            jasmine.objectContaining({ Query: 'aplicado', StoreCod: 'T001', Page: 2 })
        );

        component.filter(1);
        await Promise.resolve();
        expect(services.reportService.findSales.calls.mostRecent().args[0]).toEqual(
            jasmine.objectContaining({ Query: 'aplicado', StoreCod: 'T001', Page: 1 })
        );
    });

    it('prevents pagination during export', async () => {
        await component.findAll(1, '');
        services.reportService.findSales.calls.reset();
        component.exporting = true;
        component.filter(2);
        expect(services.reportService.findSales).not.toHaveBeenCalled();
    });

    it('exports all returned rows with the displayed columns and applied filters', async () => {
        await component.findAll(1, 'aplicado');
        component.filters.Query = 'sin aplicar';
        const exportPage = Object.assign(page(), {
            resultSearch: [{ SaleCod: 'S001', Amount: 123.45 }, { SaleCod: 'S002', Amount: 99.50 }],
            TotalResult: 2
        });
        services.reportService.exportSales.and.resolveTo({ ErrorStatus: false, Data: exportPage });
        await component.exportExcel();

        expect(services.reportService.exportSales).toHaveBeenCalledWith(
            jasmine.objectContaining({ Query: 'aplicado', Page: 1 })
        );
        const [table, filters] = services.reportExcelService.download.calls.mostRecent().args;
        expect(table instanceof DataTablaGeneticDto).toBeTrue();
        expect(table.Headers).toBe(component.dataTablaGenetic.Headers);
        expect(table.DataTable.data).toBe(exportPage);
        expect(filters.Query).toBe('aplicado');
    });

    it('ignores responses after leaving the page', async () => {
        let resolve!: (response: any) => void;
        services.reportService.findSales.and.returnValue(new Promise(done => resolve = done));
        const request = component.findAll(1, '');
        component.ngOnDestroy();
        resolve({ ErrorStatus: false, Data: page() });
        await request;
        expect(component.hasSearched).toBeFalse();
        expect(component.responsePageSearch.resultSearch).toEqual([]);
    });

    it('allows current stock without date filters', async () => {
        const stockService = jasmine.createSpyObj('ReportService', ['findStock', 'exportStock']);
        stockService.findStock.and.resolveTo({ ErrorStatus: false, Data: new ResponsePageSearch() });
        const stock = new StockReportComponent(
            stockService, services.reportExcelService, services.dataSesionService as any, services.toastrService, services.storeService
        );
        stock.resetFilters();
        await stock.findAll(1, '');
        expect(stockService.findStock).toHaveBeenCalledWith(
            jasmine.objectContaining({ DateFrom: '', DateTo: '', StoreCod: 'T001' })
        );
        expect(services.toastrService.error).not.toHaveBeenCalled();
    });

    it('shows unknown profit separately from zero without assuming a currency', () => {
        const profit = new ProfitReportComponent(
            {} as any, services.reportExcelService, services.dataSesionService as any,
            services.currencyService, services.toastrService, services.storeService
        );
        profit.loadingTable(new ResponsePageSearch<ReportRowDto>());
        const column = profit.dataTablaGenetic.Headers.find(header => header.key === 'Profit')!;
        expect(column.FunctionKey!({ Profit: null })).toBe('—');
        expect(column.FunctionKey!({ Profit: 0 })).toBe('0.00');
        expect(column.FunctionKey!({ Profit: 1250.25, CurrencyCod: 'USD' })).toBe('1,250.25');
    });
});

describe('SalesReportComponent with the shared table', () => {
    let services: ReturnType<typeof dependencies>;

    beforeEach(async () => {
        services = dependencies();
        await TestBed.configureTestingModule({
            imports: [CommonModule, FormsModule],
            declarations: [SalesReportComponent, TableComponent, FormatoMonedaPeruanaPipe],
            providers: [
                { provide: ReportService, useValue: services.reportService },
                { provide: ReportExcelService, useValue: services.reportExcelService },
                { provide: DataSesionService, useValue: services.dataSesionService },
                { provide: CurrencyService, useValue: services.currencyService },
                { provide: ToastrService, useValue: services.toastrService },
                { provide: StoreService, useValue: services.storeService }
            ]
        }).compileComponents();
    });

    it('renders rows and handles pagination directly through ActionTableService', async () => {
        const fixture = TestBed.createComponent(SalesReportComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        const table = fixture.debugElement.query(By.directive(TableComponent)).componentInstance;
        expect(table.actionTableService).toBe(fixture.componentInstance);
        expect(fixture.nativeElement.querySelector('tbody').textContent).toContain('S001');
        expect(fixture.nativeElement.querySelector('tbody').textContent).toContain('123.45');
        expect(fixture.nativeElement.querySelector('tbody').textContent).not.toContain('S/.');

        const input: HTMLInputElement = fixture.nativeElement.querySelector('#report-query');
        input.value = 'sin aplicar';
        input.dispatchEvent(new Event('input'));
        await fixture.whenStable();
        const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.pagination button'));
        buttons.find(button => button.textContent?.trim() === '2')!.click();
        await fixture.whenStable();

        expect(services.reportService.findSales.calls.mostRecent().args[0]).toEqual(
            jasmine.objectContaining({ Query: '', StoreCod: 'T001', Page: 2 })
        );
    });

    it('submits edited filters through the page form and restarts at page one', async () => {
        const fixture = TestBed.createComponent(SalesReportComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        const input: HTMLInputElement = fixture.nativeElement.querySelector('#report-query');
        input.value = ' nueva búsqueda ';
        input.dispatchEvent(new Event('input'));
        await fixture.whenStable();
        const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
        form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
        await fixture.whenStable();

        expect(services.reportService.findSales.calls.mostRecent().args[0]).toEqual(
            jasmine.objectContaining({ Query: 'nueva búsqueda', Page: 1 })
        );
    });
});
