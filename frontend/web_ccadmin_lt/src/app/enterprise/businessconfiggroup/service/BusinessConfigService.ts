import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { BusinessConfigEntity } from "src/app/enterprise/shared/model/entity/BusinessConfigEntity";

@Injectable({
    providedIn: 'root'
})
export class BusinessConfigService {

    constructor(private apiService: ApiService) { }

    async findById(GroupCod: string, ConfigCorr: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { GroupCod: GroupCod, ConfigCorr: ConfigCorr });

        return RespuestaWS;
    }

    async findAll(Query: string, Page: number, GroupCod: string = ""): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page, GroupCod: GroupCod });

        return RespuestaWS;
    }

    async findDataForm(GroupCod: string = ""): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/findDataForm`;
        let request: any = { GroupCod: GroupCod };

        let RespuestaWS: ResponseWsDto;
        RespuestaWS = await this.apiService.ExecuteGetService(url, request);

        return RespuestaWS;
    }

    async save(entity: BusinessConfigEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, entity);

        return RespuestaWS;
    }

    async enable(entity: BusinessConfigEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/enable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, entity);

        return RespuestaWS;
    }

    async disable(entity: BusinessConfigEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/disable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, entity);

        return RespuestaWS;
    }
}
