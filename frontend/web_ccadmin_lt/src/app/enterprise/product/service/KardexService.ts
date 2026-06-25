import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ICrudService } from "../../shared/interface/ICrudService";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { KardexEntity } from "../model/entity/KardexEntity";

@Injectable({
    providedIn: 'root'
})
export class KardexService implements ICrudService<KardexEntity,number>{

    constructor(private apiService: ApiService) {
    }

    FindById(Id: number): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }

    async FindAll(Search: SearchDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/kardex/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,Search);

        return RespuestaWS;
    }

    Save(Entity: KardexEntity): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    SaveAll(EntityList: KardexEntity[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    FindAllById(IdList: number[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }

    
}
