import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { AppUserEntity } from '../model/entity/AppUserEntity';
import { AppProfileEntity } from "../model/entity/AppProfileEntity";

@Injectable({
    providedIn: 'root'
})
export class AppProfileService
{
    constructor(private apiService : ApiService)
    {

    }

    async findDataForm(ProfileCod : string)
    {
        let url: string = `${AppSetting.API}/api/v1/AppProfile/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ ProfileCod : ProfileCod });

        return RespuestaWS;
    }

    async save(profile : AppProfileEntity)
    {
        let url: string = `${AppSetting.API}/api/v1/AppProfile/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,profile);

        return RespuestaWS;
    }

    async findAll(search : SearchDto)
    {
        let url: string = `${AppSetting.API}/api/v1/AppProfile/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,search);

        return RespuestaWS;
    }
}