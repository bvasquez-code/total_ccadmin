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
    selector: 'app-documentsreport',
    templateUrl: './documentsreport.component.html'
})
export class DocumentsReportComponent implements OnInit, OnDestroy, ActionTableService<ReportRowDto> {
    readonly title = "Reporte de comprobantes";
    readonly description = "Comprobantes por fecha de emisión y notas de crédito por fecha de registro. Cada documento ocupa una fila: una venta puede tener proforma y comprobante fiscal. Las notas de crédito muestran importe negativo.";
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
        data.init(
            [
                { Name: "Comprobante", key: 'DocumentCod', FunctionKey: (row: ReportRowDto) => row['DocumentCod'] ?? '—' },
                { Name: "Talonario", key: 'CounterfoilCod', FunctionKey: (row: ReportRowDto) => row['CounterfoilCod'] ?? '—' },
                { Name: "Tipo", key: 'DocumentType', IsStatus: true, Html: {}, Mask: {"99":"Proforma","01":"Factura","03":"Boleta","07":"Nota de crédito","":"—"}, FunctionKey: (row: ReportRowDto) => row['DocumentType'] ?? '' },
                { Name: "Rol", key: 'DocumentRole', IsStatus: true, Html: {}, Mask: {"I":"Interno","F":"Fiscal","O":"Otro","":"—"}, FunctionKey: (row: ReportRowDto) => row['DocumentRole'] ?? '' },
                { Name: "Operación", key: 'OperationCod', FunctionKey: (row: ReportRowDto) => row['OperationCod'] ?? '—' },
                { Name: "Fecha", key: 'ReportDate', IsDate: true, FunctionKey: (row: ReportRowDto) => row['ReportDate'] },
                { Name: "Local", key: 'StoreName', FunctionKey: (row: ReportRowDto) => row['StoreName'] ?? '—' },
                { Name: "Cliente", key: 'ClientCod', FunctionKey: (row: ReportRowDto) => row['ClientCod'] ?? '—' },
                { Name: "Moneda", key: 'CurrencyCod', FunctionKey: (row: ReportRowDto) => row['CurrencyCod'] ?? '—' },
                { Name: "Estado", key: 'DocumentStatus', IsStatus: true, Html: {}, Mask: {"P":"Pendiente","C":"Confirmado","X":"Anulado","R":"Rechazado","F":"Finalizado","":"—"}, FunctionKey: (row: ReportRowDto) => row['DocumentStatus'] ?? '' },
                { Name: "Importe", key: 'Amount', FunctionKey: (row: ReportRowDto) => amount(row['Amount']), CellClassFunction: () => 'text-right text-nowrap' }
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
            const response = await this.reportService.findDocuments(search);
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
            const response = await this.reportService.exportDocuments(filters);
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
