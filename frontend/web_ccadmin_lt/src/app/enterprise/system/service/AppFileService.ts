import { ApiService } from "../../compartido/service/api.service";
import { AppSetting } from "src/app/config/app.setting";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { AppFileDto } from "../model/dto/AppFileDto";
import { Injectable } from "@angular/core";
import { AppFileEntity } from "../model/entity/AppFileEntity";

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
        this.NormalizeFileRoute(RespuestaWS);

        return RespuestaWS;
    }

    async FindById(FileCod: string): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/appFile/findById`;
        const response: ResponseWsDto = await this.apiService.ExecuteGetService(url, { FileCod });
        this.NormalizeFileRoute(response);
        return response;
    }

    GetPublicRoute(FileCod: string): string {
        if (!FileCod) return "";
        const baseUrl = AppSetting.API.replace(/\/$/, "");
        return `${baseUrl}/api/v1/public/appFile/${encodeURIComponent(FileCod)}`;
    }

    private NormalizeFileRoute(response: ResponseWsDto): void {
        if (response.ErrorStatus || !response.Data) return;
        const appFile: AppFileEntity = Object.assign(new AppFileEntity(), response.Data);
        appFile.Route = this.GetPublicRoute(appFile.FileCod);
        response.Data = appFile;
    }
}
