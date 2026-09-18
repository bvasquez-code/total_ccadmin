import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ReportFilterDto } from '../model/dto/ReportFilterDto';

@Injectable({ providedIn: 'root' })
export class ReportService {
    constructor(private apiService: ApiService) { }

    findDataForm(): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(`${AppSetting.API}/api/v1/report/findDataForm`, {});
    }

    findSales(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/sales/findAll`, filters
        );
    }

    exportSales(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/sales/export`, filters
        );
    }

    findSoldProducts(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/sold-products/findAll`, filters
        );
    }

    exportSoldProducts(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/sold-products/export`, filters
        );
    }

    findStock(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/stock/findAll`, filters
        );
    }

    exportStock(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/stock/export`, filters
        );
    }

    findPaymentMethods(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/payment-methods/findAll`, filters
        );
    }

    exportPaymentMethods(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/payment-methods/export`, filters
        );
    }

    findDocuments(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/documents/findAll`, filters
        );
    }

    exportDocuments(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/documents/export`, filters
        );
    }

    findClients(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/clients/findAll`, filters
        );
    }

    exportClients(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/clients/export`, filters
        );
    }

    findOrders(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/orders/findAll`, filters
        );
    }

    exportOrders(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/orders/export`, filters
        );
    }

    findCreditNotes(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/credit-notes/findAll`, filters
        );
    }

    exportCreditNotes(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/credit-notes/export`, filters
        );
    }

    findProfit(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/profit/findAll`, filters
        );
    }

    exportProfit(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/profit/export`, filters
        );
    }

    overviewProfit(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/profit/overview`, filters
        );
    }

    findPurchaseSales(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/purchase-sales/findAll`, filters
        );
    }

    overviewPurchaseSales(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/purchase-sales/overview`, filters
        );
    }

    exportPurchaseSales(filters: ReportFilterDto): Promise<ResponseWsDto> {
        return this.apiService.ExecuteGetService(
            `${AppSetting.API}/api/v1/report/purchase-sales/export`, filters
        );
    }
}
