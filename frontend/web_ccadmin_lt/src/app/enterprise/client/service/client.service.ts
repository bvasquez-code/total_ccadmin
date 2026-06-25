import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { ClientEntity } from '../model/entity/ClientEntity';

@Injectable({
    providedIn: 'root'
})
export class ClientService{
    

    public constructor(private apiService : ApiService)
    {

    }

    async findAll(search : SearchDto)
    {
        let url: string = `${AppSetting.API}/api/v1/client/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,search);

        return RespuestaWS;
    }

    async findByDocumentNum(DocumentType : string,DocumentNum : string)
    {
        let url: string = `${AppSetting.API}/api/v1/client/findByDocumentNum`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{
            DocumentType : DocumentType,
            DocumentNum : DocumentNum
        });

        return RespuestaWS;
    }

    async Save(Client : ClientEntity)
    {
        let url: string = `${AppSetting.API}/api/v1/client/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,Client);

        return RespuestaWS;
    }

    async findDataForm(ClientCod: string): Promise<ResponseWsDto> {

        let url: string = `${AppSetting.API}/api/v1/client/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{
            ClientCod : ClientCod
        });

        return RespuestaWS;
    }

    

}