import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { AppUserEntity } from '../model/entity/AppUserEntity';

@Injectable({
    providedIn: 'root'
})
export class AppUserService
{

    constructor(private apiService: ApiService) {
    }

    async findAll(search : SearchDto)
    {
        let url: string = `${AppSetting.API}/api/v1/AppUser/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,search);

        return RespuestaWS;
    }

    async findDataForm(UserCod : string)
    {
        let url: string = `${AppSetting.API}/api/v1/AppUser/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ UserCod : UserCod });

        return RespuestaWS;
    }

    async save(AppUser : AppUserEntity)
    {
        let url: string = `${AppSetting.API}/api/v1/AppUser/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,AppUser);

        return RespuestaWS;
    }

    
}