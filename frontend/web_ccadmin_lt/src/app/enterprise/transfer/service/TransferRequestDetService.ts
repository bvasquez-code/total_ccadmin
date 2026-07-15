import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { TransferRequestDetSaveDto } from "../model/dto/TransferRequestDetSaveDto";

@Injectable({
    providedIn: 'root'
})
export class TransferRequestDetService {

    constructor(private apiService: ApiService) {
    }

    async Save(entity: TransferRequestDetSaveDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request-det/save`;
        return await this.apiService.ExecutePostService(url, entity);
    }

    async Delete(entity: TransferRequestDetSaveDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request-det/delete`;
        return await this.apiService.ExecutePostService(url, entity);
    }
}
