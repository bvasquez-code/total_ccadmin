import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from '../../shared/model/dto/SearchDto';
import { ICrudService } from "../../shared/interface/ICrudService";
import { PucharseRequestRegisterDto } from "../model/dto/PucharseRequestRegisterDto";

@Injectable({
    providedIn: 'root'
})
export class PucharseRequestHeadService implements ICrudService<PucharseRequestRegisterDto,String>
{
    constructor(private apiService: ApiService) {
    }

    FindById(Id: String): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    async FindAll(Search: SearchDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/pucharserequest/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,Search);

        return RespuestaWS;
    }

    async Save(Entity: PucharseRequestRegisterDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/pucharserequest/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,Entity);

        return RespuestaWS;
    }

    SaveAll(EntityList: PucharseRequestRegisterDto[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    FindAllById(IdList: String[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }

    async FindDataForm(PucharseReqCod : string)
    {
        let url: string = `${AppSetting.API}/api/v1/pucharserequest/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ PucharseReqCod : PucharseReqCod});

        return RespuestaWS;
    }

}