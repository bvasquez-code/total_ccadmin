import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { CashRegisterEntity } from "../model/entity/CashRegisterEntity";

@Injectable({
    providedIn: 'root'
})
export class CashregisterService {

    constructor(private apiService: ApiService) { }

    async findById(RegisterCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { RegisterCod: RegisterCod });

        return RespuestaWS;
    }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async findActives(): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/findActives`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }

    async findActivesByStore(StoreCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/findActivesByStore`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { StoreCod: StoreCod });

        return RespuestaWS;
    }

    async save(entity: CashRegisterEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, entity);

        return RespuestaWS;
    }

    async saveAll(list: CashRegisterEntity[]): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/saveAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, list);

        return RespuestaWS;
    }

    async enable(RegisterCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/enable`;
        let RespuestaWS: ResponseWsDto;

        // Backend usa @RequestParam -> tu ApiService normalmente lo serializa como params (como en otros módulos)
        RespuestaWS = await this.apiService.ExecutePostService(url, { RegisterCod: RegisterCod });

        return RespuestaWS;
    }

    async disable(RegisterCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/disable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, { RegisterCod: RegisterCod });

        return RespuestaWS;
    }

    async findDataForm(RegisterCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/register/findDataForm`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { RegisterCod: RegisterCod });

        return RespuestaWS;
    }
}
