import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { PucharseRequestDetSaveDto } from "../model/dto/PucharseRequestDetSaveDto";

@Injectable({
    providedIn: 'root'
})
export class PucharseRequestDetService
{
    constructor(private apiService: ApiService) {
    }

    async Save(Entity: PucharseRequestDetSaveDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/pucharserequestdet/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,Entity);

        return RespuestaWS;
    }

    async Delete(Entity: PucharseRequestDetSaveDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/pucharserequestdet/delete`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,Entity);

        return RespuestaWS;
    }
}
