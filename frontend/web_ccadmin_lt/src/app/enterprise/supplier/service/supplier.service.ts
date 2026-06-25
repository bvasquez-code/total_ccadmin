import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { SupplierEntity } from '../model/entity/SupplierEntity';

@Injectable({
    providedIn: 'root'
})
export class SupplierService{
    

    public constructor(private apiService : ApiService)
    {

    }

    async findAll(search : SearchDto)
    {
        let url: string = `${AppSetting.API}/api/v1/supplier/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,search);

        return RespuestaWS;
    }

    async findByDocumentNum(DocumentType : string,DocumentNum : string)
    {
        let url: string = `${AppSetting.API}/api/v1/supplier/findByDocumentNum`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{
            DocumentType : DocumentType,
            DocumentNum : DocumentNum
        });

        return RespuestaWS;
    }

    async Save(Supplier : SupplierEntity)
    {
        let url: string = `${AppSetting.API}/api/v1/supplier/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,Supplier);

        return RespuestaWS;
    }

    async findDataForm(SupplierCod: string): Promise<ResponseWsDto> {

        let url: string = `${AppSetting.API}/api/v1/supplier/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{
            SupplierCod : SupplierCod
        });

        return RespuestaWS;
    }

}
