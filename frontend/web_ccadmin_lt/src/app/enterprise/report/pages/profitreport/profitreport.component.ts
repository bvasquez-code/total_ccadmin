import { Component, OnDestroy, OnInit } from '@angular/core';
import { StoreEntity } from '../../../shared/model/entity/StoreEntity';
import { formatNumber } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { ActionTableService } from '../../../shared/interface/ActionTableService';
import { DataTablaGeneticDto } from '../../../shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { CurrencyEntity } from '../../../shared/model/entity/CurrencyEntity';
import { FinancialReportOverviewDto, FinancialReportSummaryDto } from '../../model/dto/FinancialReportOverviewDto';
import { ReportPeriodDto } from '../../model/dto/ReportPeriodDto';
import { ReportTrendDto } from '../../model/dto/ReportTrendDto';
import { ReportRowDto } from '../../model/dto/ReportRowDto';
import { ReportFilterDto } from '../../model/dto/ReportFilterDto';
import { ReportService } from '../../service/report.service';
import { ReportExcelService } from '../../service/report-excel.service';

@Component({
    selector: 'app-profitreport',
    templateUrl: './profitreport.component.html',
    styleUrls: ['../../financial-report.css']
})
export class ProfitReportComponent implements OnInit, OnDestroy, ActionTableService<ReportRowDto> {
    readonly title = 'Utilidad por venta';
    readonly description = 'Importe vendido menos el costo trazado de esas unidades, aunque se compraran en otro mes. Incluye notas de crédito y costo recuperado del stock aceptado. Importes registrados, con impuestos cuando estén incluidos; excluye gastos operativos.';
    period = new ReportPeriodDto();
    readonly months = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    overviewData?: FinancialReportOverviewDto;
    summaryCurrency = '';
    cumulative = true;
    trend?: ReportTrendDto;

    get summary(): FinancialReportSummaryDto | undefined {
        return this.overviewData?.Summary.find(item => item.CurrencyCod === this.summaryCurrency);
    }

    updateChart(): void {
        if (!this.overviewData || !this.appliedFilters) return;
        this.trend = new ReportTrendDto(this.overviewData.Daily,
            this.appliedFilters.DateFrom, this.appliedFilters.DateTo,
            this.summaryCurrency, this.cumulative, 'PROFIT');
    }

    responsePageSearch = new ResponsePageSearch<ReportRowDto>();
    dataTablaGenetic = new DataTablaGeneticDto<ReportRowDto>();
    filters = new ReportFilterDto();
    appliedFilters?: ReportFilterDto;
    storeList: StoreEntity[] = [];
    loadingFormData = false;
    currencyList: CurrencyEntity[] = [];
    hasSearched = false;
    loading = false;
    exporting = false;
    errorMessage = '';
    private requestNumber = 0;
    private destroyed = false;

    constructor(
        private reportService: ReportService,
        private reportExcelService: ReportExcelService,
        private dataSesionService: DataSesionService,
        private toastrService: ToastrService
    ) { }

    ngOnInit(): void {
        void this.findDataForm();
        this.resetFilters();
        this.loadingTable(this.responsePageSearch);
        void this.findAll(1, this.filters.Query);
    }

    resetFilters(): void {
        this.period = new ReportPeriodDto();
        this.filters = new ReportFilterDto(
            this.dataSesionService.getSessionStorageDto().StoreCod || '', '');
        this.period.apply(this.filters);
    }

    filter(Page: number): void {
        if (Page > 0 && !this.loading && !this.exporting && this.appliedFilters) {
            void this.findAll(Page, this.appliedFilters.Query, this.appliedFilters);
        }
    }

    loadingTable(responsePageSearch: ResponsePageSearch<ReportRowDto>): void {
        const data = new DataTablaGeneticDto<ReportRowDto>();
        const amount = (value: string | number | null) =>
            value == null ? '—' : formatNumber(Number(value), 'en-US', '1.2-2');
        const number = (value: string | number | null) =>
            value == null ? '—' : formatNumber(Number(value), 'en-US', '1.0-4');
        data.init(
            [
                { Name: "Movimiento", key: 'SourceType', IsStatus: true, Html: {}, Mask: {"SALE":"Venta","CREDIT_NOTE":"Nota de crédito","":"—"}, FunctionKey: (row: ReportRowDto) => row['SourceType'] ?? '' },
                { Name: "Operación", key: 'OperationCod', FunctionKey: (row: ReportRowDto) => row['OperationCod'] ?? '—' },
                { Name: "Ítem", key: 'ItemNumber', FunctionKey: (row: ReportRowDto) => number(row['ItemNumber']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Fecha", key: 'ReportDate', IsDate: true, FunctionKey: (row: ReportRowDto) => row['ReportDate'] },
                { Name: "Local", key: 'StoreName', FunctionKey: (row: ReportRowDto) => row['StoreName'] ?? '—' },
                { Name: "Código", key: 'ProductCod', FunctionKey: (row: ReportRowDto) => row['ProductCod'] ?? '—' },
                { Name: "Producto", key: 'ProductName', FunctionKey: (row: ReportRowDto) => row['ProductName'] ?? '—' },
                { Name: "Variante", key: 'Variant', FunctionKey: (row: ReportRowDto) => row['Variant'] ?? '—' },
                { Name: "Moneda", key: 'CurrencyCod', FunctionKey: (row: ReportRowDto) => row['CurrencyCod'] ?? '—' },
                { Name: "Cantidad interna", key: 'Quantity', FunctionKey: (row: ReportRowDto) => number(row['Quantity']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Importe vendido", key: 'Amount', FunctionKey: (row: ReportRowDto) => amount(row['Amount']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Costo trazado", key: 'Cost', FunctionKey: (row: ReportRowDto) => amount(row['Cost']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Utilidad", key: 'Profit', FunctionKey: (row: ReportRowDto) => amount(row['Profit']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Margen %", key: 'Margin', FunctionKey: (row: ReportRowDto) => amount(row['Margin']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Costo", key: 'CostStatus', IsStatus: true, Html: {}, Mask: {"COMPLETE":"Verificado","MISSING":"Pendiente","":"—"}, FunctionKey: (row: ReportRowDto) => row['CostStatus'] ?? '' }
            ],
            { data: responsePageSearch },
            this.title
        );
        this.dataTablaGenetic = data;
    }

    async findAll(Page: number, Query: string, filters: ReportFilterDto = this.filters): Promise<void> {
        if (Page < 1 || this.exporting || this.destroyed) return;
        const search = Object.assign(new ReportFilterDto(), filters, { Page, Query: Query.trim() });
        try {
            if (filters === this.filters) {
                this.period.apply(search);
                this.filters.DateFrom = search.DateFrom;
                this.filters.DateTo = search.DateTo;
            }
            ReportFilterDto.validate(search);
        } catch (error) {
            this.toastrService.error(error instanceof Error ? error.message : 'Revise los filtros');
            return;
        }
        const request = ++this.requestNumber;
        this.loading = true;
        this.errorMessage = '';
        try {
            const [response, overview] = await Promise.all([
                this.reportService.findProfit(search), this.reportService.overviewProfit(search)
            ]);
            if (request !== this.requestNumber) return;
            if (response.ErrorStatus) throw new Error(response.Message || 'No se pudo consultar el reporte');
            if (overview.ErrorStatus) throw new Error(overview.Message || 'No se pudo consultar el resumen');
            this.overviewData = overview.Data;
            this.summaryCurrency = this.overviewData!.Summary.some(item => item.CurrencyCod === this.summaryCurrency)
                ? this.summaryCurrency : (this.overviewData!.Summary[0]?.CurrencyCod || '');
            this.responsePageSearch = response.Data;
            this.loadingTable(this.responsePageSearch);
            this.appliedFilters = { ...search };
            this.updateChart();
            this.hasSearched = true;
        } catch (error) {
            if (request !== this.requestNumber) return;
            this.responsePageSearch = new ResponsePageSearch<ReportRowDto>();
            this.loadingTable(this.responsePageSearch);
            this.appliedFilters = undefined;
            this.overviewData = undefined;
            this.trend = undefined;
            this.hasSearched = false;
            this.errorMessage = error instanceof Error ? error.message : 'No se pudo consultar el reporte';
            this.toastrService.error(this.errorMessage);
        } finally {
            if (request === this.requestNumber) this.loading = false;
        }
    }

    async exportExcel(): Promise<void> {
        if (!this.appliedFilters || this.loading || this.exporting || this.destroyed) return;
        const filters = { ...this.appliedFilters, Page: 1 };
        const request = this.requestNumber;
        this.exporting = true;
        try {
            const [response, overview] = await Promise.all([
                this.reportService.exportProfit(filters), this.reportService.overviewProfit(filters)
            ]);
            if (request !== this.requestNumber) return;
            if (response.ErrorStatus) throw new Error(response.Message || 'No se pudo exportar el reporte');
            if (overview.ErrorStatus) throw new Error(overview.Message || 'No se pudo exportar el resumen');
            const exportTable = new DataTablaGeneticDto<ReportRowDto>();
            exportTable.init(this.dataTablaGenetic.Headers, { data: response.Data }, this.title);
            this.reportExcelService.download(exportTable, filters, this.description, overview.Data);
        } catch (error) {
            if (request === this.requestNumber) {
                this.toastrService.error(error instanceof Error ? error.message : 'No se pudo exportar el reporte');
            }
        } finally {
            if (request === this.requestNumber) this.exporting = false;
        }
    }

    async findDataForm(): Promise<void> {
        this.loadingFormData = true;
        try {
            const response = await this.reportService.findDataForm();
            if (this.destroyed) return;
            if (response.ErrorStatus) throw new Error(response.Message || 'No se pudieron cargar los filtros del reporte');
            this.storeList = response.DataAdditional.find(item => item.Name === 'storeList')?.Data || [];
            this.currencyList = response.DataAdditional.find(item => item.Name === 'currencyList')?.Data || [];
        } catch (error) {
            if (!this.destroyed) {
                this.toastrService.error(error instanceof Error ? error.message : 'No se pudieron cargar los filtros del reporte');
            }
        } finally {
            if (!this.destroyed) this.loadingFormData = false;
        }
    }

    getDataRow(_item: ReportRowDto): void { }

    ngOnDestroy(): void {
        this.destroyed = true;
        this.requestNumber++;
    }
}
