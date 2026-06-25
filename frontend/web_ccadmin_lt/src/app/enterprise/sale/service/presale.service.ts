import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { PresaleRegisterDto } from "../model/dto/PresaleRegisterDto";
import { SearchDto } from '../../shared/model/dto/SearchDto';

@Injectable({
    providedIn: 'root'
})
export class PresaleService
{
    constructor(private apiService: ApiService) {
    }

    async findDataForm(PresaleCod : string)
    {
        let url: string = `${AppSetting.API}/api/v1/presale/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ PresaleCod : PresaleCod });

        return RespuestaWS;
    }

    async save(presaleRegister: PresaleRegisterDto)
    {
        let url: string = `${AppSetting.API}/api/v1/presale/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,presaleRegister);

        return RespuestaWS;
    }

    async confirm(presaleRegister: PresaleRegisterDto)
    {
        let url: string = `${AppSetting.API}/api/v1/presale/confirm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,presaleRegister);

        return RespuestaWS;
    }

    async findAll(search : SearchDto)
    {
        let url: string = `${AppSetting.API}/api/v1/presale/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,search);

        return RespuestaWS;
    }

    async createCode(){
        let url: string = `${AppSetting.API}/api/v1/presale/createCode`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{});

        return RespuestaWS;
    }
}