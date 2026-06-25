import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { CurrencyEntity } from "src/app/enterprise/shared/model/entity/CurrencyEntity";

@Injectable({
    providedIn: 'root'
})
export class CurrencyService {

    constructor(private apiService: ApiService) { }

    async findById(CurrencyCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { CurrencyCod: CurrencyCod });

        return RespuestaWS;
    }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async findActives(): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/findActives`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }

    async findCurrencySystem(): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/findCurrencySystem`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }

    async findDataForm(CurrencyCod: string = ""): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/findDataForm`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { CurrencyCod: CurrencyCod });

        return RespuestaWS;
    }

    async save(currency: CurrencyEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, currency);

        return RespuestaWS;
    }

    async saveAll(currencyList: CurrencyEntity[]): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/saveAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, currencyList);

        return RespuestaWS;
    }

    async enable(currency: CurrencyEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/enable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, currency);

        return RespuestaWS;
    }

    async disable(currency: CurrencyEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/currency/disable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, currency);

        return RespuestaWS;
    }
}
