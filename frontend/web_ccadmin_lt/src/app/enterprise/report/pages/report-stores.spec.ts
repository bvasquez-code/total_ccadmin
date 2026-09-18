import { CommonModule } from '@angular/common';
import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { TableComponent } from '../../shared/component/table/table.component';
import { ResponsePageSearch } from '../../shared/model/dto/ResponsePageSearch';
import { FormatoMonedaPeruanaPipe } from '../../shared/pipe/FormatoMonedaPeruana.pipe';
import { StoreService } from '../../store/service/store.service';
import { CurrencyService } from '../../system/service/CurrencyService';
import { ReportExcelService } from '../service/report-excel.service';
import { ReportService } from '../service/report.service';
import { ClientsReportComponent } from './clientsreport/clientsreport.component';
import { CreditNotesReportComponent } from './creditnotesreport/creditnotesreport.component';
import { DocumentsReportComponent } from './documentsreport/documentsreport.component';
import { OrdersReportComponent } from './ordersreport/ordersreport.component';
import { PaymentMethodsReportComponent } from './paymentmethodsreport/paymentmethodsreport.component';
import { ProfitReportComponent } from './profitreport/profitreport.component';
import { PurchaseSalesReportComponent } from './purchasesalesreport/purchasesalesreport.component';
import { SalesReportComponent } from './salesreport/salesreport.component';
import { SoldProductsReportComponent } from './soldproductsreport/soldproductsreport.component';
import { StockReportComponent } from './stockreport/stockreport.component';

const reports: { component: Type<any>; method: string; financial?: boolean }[] = [
    { component: SalesReportComponent, method: 'Sales' },
    { component: SoldProductsReportComponent, method: 'SoldProducts' },
    { component: StockReportComponent, method: 'Stock' },
    { component: PaymentMethodsReportComponent, method: 'PaymentMethods' },
    { component: DocumentsReportComponent, method: 'Documents' },
    { component: ClientsReportComponent, method: 'Clients' },
    { component: OrdersReportComponent, method: 'Orders' },
    { component: CreditNotesReportComponent, method: 'CreditNotes' },
    { component: ProfitReportComponent, method: 'Profit', financial: true },
    { component: PurchaseSalesReportComponent, method: 'PurchaseSales', financial: true }
];

for (const report of reports) {
    describe(`${report.component.name}: tiendas`, () => {
        let storeService: jasmine.SpyObj<StoreService>;
        let reportService: jasmine.SpyObj<any>;
        let toastrService: jasmine.SpyObj<ToastrService>;

        beforeEach(async () => {
            storeService = jasmine.createSpyObj('StoreService', ['FindAllList']);
            storeService.FindAllList.and.resolveTo({ ErrorStatus: false, Data: [
                { StoreCod: 'T001', Name: 'Tienda uno', Status: 'A' },
                { StoreCod: 'T002', Name: 'Tienda dos', Status: 'A' },
                { StoreCod: 'T003', Name: 'Inactiva', Status: 'I' }
            ] } as any);
            reportService = jasmine.createSpyObj('ReportService', [
                `find${report.method}`, `export${report.method}`, `overview${report.method}`
            ]);
            const page = { ErrorStatus: false, Data: new ResponsePageSearch() };
            reportService[`find${report.method}`].and.resolveTo(page);
            reportService[`export${report.method}`].and.resolveTo(page);
            reportService[`overview${report.method}`].and.resolveTo({ ErrorStatus: false, Data: { Summary: [], Daily: [] } });
            toastrService = jasmine.createSpyObj('ToastrService', ['error']);
            await TestBed.configureTestingModule({
                imports: [CommonModule, FormsModule],
                declarations: [report.component, TableComponent, FormatoMonedaPeruanaPipe],
                providers: [
                    { provide: StoreService, useValue: storeService },
                    { provide: ReportService, useValue: reportService },
                    { provide: ReportExcelService, useValue: jasmine.createSpyObj('ReportExcelService', ['download']) },
                    { provide: ToastrService, useValue: toastrService },
                    { provide: DataSesionService, useValue: { getSessionStorageDto: () => ({ StoreCod: 'T001' }) } },
                    { provide: CurrencyService, useValue: { findActives: async () => ({ ErrorStatus: false, Data: [] }) } }
                ]
            }).compileComponents();
        });

        it('loads the catalog without a session StoreList and submits one store or all stores', async () => {
            const fixture = TestBed.createComponent(report.component);
            fixture.detectChanges();
            await fixture.whenStable();
            fixture.detectChanges();
            await fixture.whenStable();
            const select: HTMLSelectElement = fixture.nativeElement.querySelector('#report-store');
            expect(storeService.FindAllList).toHaveBeenCalledTimes(1);
            expect(Array.from(select.options).map(option => option.value)).toEqual(['', 'T001', 'T002']);
            expect(select.options[0].textContent).toContain('Todas las tiendas');
            expect(select.value).toBe('T001');
            expect(select.disabled).toBeFalse();

            for (const storeCod of ['T002', '']) {
                select.value = storeCod;
                select.dispatchEvent(new Event('change'));
                await fixture.whenStable();
                fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
                await fixture.whenStable();
                fixture.detectChanges();
                expect(reportService[`find${report.method}`].calls.mostRecent().args[0].StoreCod).toBe(storeCod);
                if (report.financial) {
                    expect(reportService[`overview${report.method}`].calls.mostRecent().args[0].StoreCod).toBe(storeCod);
                }
            }

            fixture.componentInstance.filters.StoreCod = 'T001';
            const search = spyOn(fixture.componentInstance, 'findAll').and.callThrough();
            fixture.componentInstance.filter(2);
            await search.calls.mostRecent().returnValue;
            expect(reportService[`find${report.method}`].calls.mostRecent().args[0].StoreCod).toBe('');
            await fixture.componentInstance.exportExcel();
            expect(reportService[`export${report.method}`].calls.mostRecent().args[0].StoreCod).toBe('');
            expect(toastrService.error).not.toHaveBeenCalled();
        });

        it('shows catalog errors instead of consuming failed responses', async () => {
            storeService.FindAllList.and.resolveTo({ ErrorStatus: true, Message: 'Tiendas no disponibles', Data: {} } as any);
            const fixture = TestBed.createComponent(report.component);
            fixture.detectChanges();
            await fixture.whenStable();
            expect(fixture.componentInstance.storeList).toEqual([]);
            expect(fixture.componentInstance.loadingStores).toBeFalse();
            expect(toastrService.error).toHaveBeenCalledWith('Tiendas no disponibles');
        });
    });
}
