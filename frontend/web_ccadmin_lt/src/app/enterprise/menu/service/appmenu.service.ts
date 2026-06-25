import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { AppMenuEntity } from "../model/entity/AppMenuEntity";
import { SearchDto } from "../../shared/model/dto/SearchDto";

@Injectable({
    providedIn: 'root'
})

export class AppMenuService {

    constructor(private apiService: ApiService) {
    }

    async findAll(search: SearchDto) {
        let url: string = `${AppSetting.API}/api/v1/appMenu/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, search);

        return RespuestaWS;
    }

    async findDataForm(Id: string) {
        let url: string = `${AppSetting.API}/api/v1/appMenu/findDataForm`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Id: Id });

        return RespuestaWS;
    }

    async save(appMenu: AppMenuEntity) {
        let url: string = `${AppSetting.API}/api/v1/appMenu/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, appMenu);

        return RespuestaWS;
    }

    async updateStatus(appMenu: AppMenuEntity) {
        let url: string = `${AppSetting.API}/api/v1/appMenu/updateStatus`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, appMenu);

        return RespuestaWS;
    }

    async FindById(MenuCod: string) {
        let url: string = `${AppSetting.API}/api/v1/appMenu/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { MenuCod: MenuCod });

        return RespuestaWS;
    }


}