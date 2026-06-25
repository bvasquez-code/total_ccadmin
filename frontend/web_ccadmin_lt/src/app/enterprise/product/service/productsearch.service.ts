import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";

@Injectable({
    providedIn: 'root'
})

export class ProductSearchService
{

    constructor(private apiService: ApiService) {
    }

    async query(Request: any)
    {
        let url: string = `${AppSetting.API}/api/v1/productSearch/query`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,Request);

        return RespuestaWS;
    }
}