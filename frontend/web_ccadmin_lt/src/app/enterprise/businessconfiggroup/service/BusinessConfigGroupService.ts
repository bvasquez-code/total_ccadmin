import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { BusinessConfigGroupEntity } from "../model/entity/BusinessConfigGroupEntity";

@Injectable({
    providedIn: 'root'
})
export class BusinessConfigGroupService {

    constructor(private apiService: ApiService) { }

    async findById(GroupCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { GroupCod: GroupCod });

        return RespuestaWS;
    }

    async findByGroupId(GroupId: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/findByGroupId`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { GroupId: GroupId });

        return RespuestaWS;
    }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async findActives(): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/findActives`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }

    async findDataForm(GroupCod: string = ""): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/findDataForm`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { GroupCod: GroupCod });

        return RespuestaWS;
    }

    async save(entity: BusinessConfigGroupEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, entity);

        return RespuestaWS;
    }

    async saveAll(list: BusinessConfigGroupEntity[]): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/saveAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, list);

        return RespuestaWS;
    }

    async enable(entity: BusinessConfigGroupEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/enable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, entity);

        return RespuestaWS;
    }

    async disable(entity: BusinessConfigGroupEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/business/config/group/disable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, entity);

        return RespuestaWS;
    }
}
