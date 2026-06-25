import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from '../../shared/model/dto/SearchDto';
import { ICrudService } from "../../shared/interface/ICrudService";
import { PucharseDetEntity } from "../model/entity/PucharseDetEntity";
import { PucharseDetConfirmDto } from "../model/dto/PucharseDetConfirmDto";

@Injectable({
    providedIn: 'root'
})
export class PucharseDetService implements ICrudService<PucharseDetEntity,PucharseDetEntity>
{
    constructor(private apiService: ApiService) {
    }

    FindById(Id: PucharseDetEntity): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    FindAll(Search: SearchDto): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    Save(Entity: PucharseDetEntity): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    SaveAll(EntityList: PucharseDetEntity[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }
    FindAllById(IdList: PucharseDetEntity[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }

    async Confirm(pucharseDetConfirm: PucharseDetConfirmDto): Promise<ResponseWsDto> {
        
        let url: string = `${AppSetting.API}/api/v1/PucharseDet/confirm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,pucharseDetConfirm);

        return RespuestaWS;

    }


}