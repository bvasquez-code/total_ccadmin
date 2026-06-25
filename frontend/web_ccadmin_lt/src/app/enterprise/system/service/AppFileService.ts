import { ApiService } from "../../compartido/service/api.service";
import { AppSetting } from "src/app/config/app.setting";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { AppFileDto } from "../model/dto/AppFileDto";
import { Injectable } from "@angular/core";

@Injectable({
    providedIn: 'root'
})
export class AppFileService {

    constructor(private apiService: ApiService) {
    }

    async Save(AppFile: AppFileDto): Promise<ResponseWsDto> {

        let url: string = `${AppSetting.API}/api/v1/appFile/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,AppFile);

        return RespuestaWS;
    }
}
