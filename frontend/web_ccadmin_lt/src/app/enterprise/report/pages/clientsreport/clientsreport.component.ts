import { Component, OnDestroy, OnInit } from '@angular/core';
import { StoreEntity } from '../../../shared/model/entity/StoreEntity';
import { formatNumber } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { ActionTableService } from '../../../shared/interface/ActionTableService';
import { DataTablaGeneticDto } from '../../../shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { CurrencyEntity } from '../../../shared/model/entity/CurrencyEntity';
import { ReportRowDto } from '../../model/dto/ReportRowDto';
import { ReportFilterDto } from '../../model/dto/ReportFilterDto';
import { ReportService } from '../../service/report.service';
import { ReportExcelService } from '../../service/report-excel.service';

@Component({
    selector: 'app-clientsreport',
    templateUrl: './clientsreport.component.html'
})
export class ClientsReportComponent implements OnInit, OnDestroy, ActionTableService<ReportRowDto> {
    readonly title = "Reporte de clientes";
    readonly description = "Clientes con ventas confirmadas en el período, agrupados por local y moneda. Incluye frecuencia de compra, primera y última venta del período, consumo y ticket promedio antes de notas de crédito.";
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
        this.filters = new ReportFilterDto(
            this.dataSesionService.getSessionStorageDto().StoreCod || '', '');
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
                { Name: "Código", key: 'ClientCod', FunctionKey: (row: ReportRowDto) => row['ClientCod'] ?? '—' },
                { Name: "Cliente", key: 'ClientName', FunctionKey: (row: ReportRowDto) => row['ClientName'] ?? '—' },
                { Name: "Documento", key: 'DocumentNum', FunctionKey: (row: ReportRowDto) => row['DocumentNum'] ?? '—' },
                { Name: "Local", key: 'StoreName', FunctionKey: (row: ReportRowDto) => row['StoreName'] ?? '—' },
                { Name: "Moneda", key: 'CurrencyCod', FunctionKey: (row: ReportRowDto) => row['CurrencyCod'] ?? '—' },
                { Name: "Compras", key: 'SaleCount', FunctionKey: (row: ReportRowDto) => number(row['SaleCount']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Primera compra", key: 'FirstSaleDate', IsDate: true, FunctionKey: (row: ReportRowDto) => row['FirstSaleDate'] },
                { Name: "Última compra", key: 'LastSaleDate', IsDate: true, FunctionKey: (row: ReportRowDto) => row['LastSaleDate'] },
                { Name: "Total comprado", key: 'Amount', FunctionKey: (row: ReportRowDto) => amount(row['Amount']), CellClassFunction: () => 'text-right text-nowrap' },
                { Name: "Ticket promedio", key: 'AverageTicket', FunctionKey: (row: ReportRowDto) => amount(row['AverageTicket']), CellClassFunction: () => 'text-right text-nowrap' }
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
            ReportFilterDto.validate(search);
        } catch (error) {
            this.toastrService.error(error instanceof Error ? error.message : 'Revise los filtros');
            return;
        }
        const request = ++this.requestNumber;
        this.loading = true;
        this.errorMessage = '';
        try {
            const response = await this.reportService.findClients(search);
            if (request !== this.requestNumber) return;
            if (response.ErrorStatus) throw new Error(response.Message || 'No se pudo consultar el reporte');
            this.responsePageSearch = response.Data;
            this.loadingTable(this.responsePageSearch);
            this.appliedFilters = { ...search };
            this.hasSearched = true;
        } catch (error) {
            if (request !== this.requestNumber) return;
            this.responsePageSearch = new ResponsePageSearch<ReportRowDto>();
            this.loadingTable(this.responsePageSearch);
            this.appliedFilters = undefined;
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
            const response = await this.reportService.exportClients(filters);
            if (request !== this.requestNumber) return;
            if (response.ErrorStatus) throw new Error(response.Message || 'No se pudo exportar el reporte');
            const exportTable = new DataTablaGeneticDto<ReportRowDto>();
            exportTable.init(this.dataTablaGenetic.Headers, { data: response.Data }, this.title);
            this.reportExcelService.download(exportTable, filters, this.description);
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
