import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ICrudService } from "../../shared/interface/ICrudService";
import { PucharseRegisterDto } from "../model/dto/PucharseRegisterDto";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { PucharseHeadEntity } from "../model/entity/PucharseHeadEntity";

@Injectable({
    providedIn: 'root'
})
export class PucharseService implements ICrudService<PucharseRegisterDto,String>
{
    constructor(private apiService: ApiService) {
    }

    FindById(Id: String): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }

    async FindAll(Search: SearchDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/pucharse/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,Search);

        return RespuestaWS;
    }

    async Save(Entity: PucharseRegisterDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/pucharse/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,Entity);

        return RespuestaWS;
    }

    SaveAll(EntityList: PucharseRegisterDto[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    FindAllById(IdList: String[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }

    async FindDataForm(PucharseCod: String): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/pucharse/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ PucharseCod : PucharseCod});

        return RespuestaWS;
    }

    async EndReception(pucharseHead : PucharseHeadEntity)
    {
        let url: string = `${AppSetting.API}/api/v1/pucharse/endReception`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,pucharseHead);

        return RespuestaWS;
    }

}