import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";

@Injectable({
    providedIn: 'root'
})
export class PersonService{
    

    public constructor(private apiService : ApiService)
    {

    }

    async findByDocumentNum(DocumentType : string,DocumentNum : string)
    {
        let url: string = `${AppSetting.API}/api/v1/person/findByDocumentNum`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{
            DocumentType : DocumentType,
            DocumentNum : DocumentNum
        });

        return RespuestaWS;
    }

}
