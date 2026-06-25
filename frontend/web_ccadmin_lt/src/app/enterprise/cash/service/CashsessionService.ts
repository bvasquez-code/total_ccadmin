import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { OpenRequestDto } from "../model/dto/OpenRequestDto";
import { CloseRequestDto } from "../model/dto/CloseRequestDto";
import { CashSessionItemEntity } from "../model/entity/CashSessionItemEntity";

@Injectable({
    providedIn: 'root'
})
export class CashsessionService {

    constructor(private apiService: ApiService) { }

    async open(req: OpenRequestDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/session/open`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, req);

        return RespuestaWS;
    }

    async close(req: CloseRequestDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/session/close`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, req);

        return RespuestaWS;
    }

    async getItems(CashSessionID: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/session/items`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { CashSessionID: CashSessionID });

        return RespuestaWS;
    }

    async addItem(item: CashSessionItemEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/session/item/add`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, item);

        return RespuestaWS;
    }

    async addAll(items: CashSessionItemEntity[]): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/cash/session/item/addAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, items);

        return RespuestaWS;
    }
}
