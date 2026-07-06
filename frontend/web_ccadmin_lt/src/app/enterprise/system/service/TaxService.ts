import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { TaxEntity } from "../model/entity/TaxEntity";

@Injectable({
    providedIn: 'root'
})
export class TaxService {

    constructor(private apiService: ApiService) { }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/tax/findAll`;
        return await this.apiService.ExecuteGetService(url, { Query, Page });
    }

    async findActives(): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/tax/findActives`;
        return await this.apiService.ExecuteGetService(url, {});
    }

    async findDataForm(TaxCod: string = ""): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/tax/findDataForm`;
        return await this.apiService.ExecuteGetService(url, { TaxCod });
    }

    async save(tax: TaxEntity): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/tax/save`;
        return await this.apiService.ExecutePostService(url, tax);
    }

    async enable(tax: TaxEntity): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/tax/enable`;
        return await this.apiService.ExecutePostService(url, tax);
    }

    async disable(tax: TaxEntity): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/tax/disable`;
        return await this.apiService.ExecutePostService(url, tax);
    }
}
