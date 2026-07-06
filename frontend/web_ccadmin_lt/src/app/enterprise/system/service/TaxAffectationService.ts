import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { TaxAffectationEntity } from "../model/entity/TaxAffectationEntity";

@Injectable({
    providedIn: 'root'
})
export class TaxAffectationService {

    constructor(private apiService: ApiService) { }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/taxAffectation/findAll`;
        return await this.apiService.ExecuteGetService(url, { Query, Page });
    }

    async findActives(): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/taxAffectation/findActives`;
        return await this.apiService.ExecuteGetService(url, {});
    }

    async findDataForm(TaxAffectationCod: string = ""): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/taxAffectation/findDataForm`;
        return await this.apiService.ExecuteGetService(url, { TaxAffectationCod });
    }

    async save(taxAffectation: TaxAffectationEntity): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/taxAffectation/save`;
        return await this.apiService.ExecutePostService(url, taxAffectation);
    }

    async enable(taxAffectation: TaxAffectationEntity): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/taxAffectation/enable`;
        return await this.apiService.ExecutePostService(url, taxAffectation);
    }

    async disable(taxAffectation: TaxAffectationEntity): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/taxAffectation/disable`;
        return await this.apiService.ExecutePostService(url, taxAffectation);
    }
}
