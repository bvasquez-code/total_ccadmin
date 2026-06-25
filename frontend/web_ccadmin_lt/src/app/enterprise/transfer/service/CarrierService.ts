import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { CarrierEntity } from "../model/entity/CarrierEntity";

@Injectable({
    providedIn: 'root'
})
export class CarrierService {

    private readonly baseUrl: string = `${AppSetting.API}/api/v1/transfers/carriers`;

    constructor(
        private apiService: ApiService
    ) {
    }

    async findById(CarrierCod: string): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/findById`;
        return await this.apiService.ExecuteGetService(url, { CarrierCod: CarrierCod });
    }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/findAll`;
        return await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });
    }

    async findActives(): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/findActives`;
        return await this.apiService.ExecuteGetService(url, {});
    }

    async save(entity: CarrierEntity): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/save`;
        return await this.apiService.ExecutePostService(url, entity);
    }

    async saveAll(list: CarrierEntity[]): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/saveAll`;
        return await this.apiService.ExecutePostService(url, list);
    }

    async enable(CarrierCod: string): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/enable?CarrierCod=${encodeURIComponent(CarrierCod)}`;
        return await this.apiService.ExecutePostService(url, {});
    }

    async disable(CarrierCod: string): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/disable?CarrierCod=${encodeURIComponent(CarrierCod)}`;
        return await this.apiService.ExecutePostService(url, {});
    }

    async delete(carrierCod: string): Promise<ResponseWsDto> {
        const url: string = `${this.baseUrl}/${encodeURIComponent(carrierCod)}`;
        return await this.apiService.ExecuteDeleteService(url);
    }
}
