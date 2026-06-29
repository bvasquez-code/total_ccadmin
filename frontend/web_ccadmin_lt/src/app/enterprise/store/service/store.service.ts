import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { StoreEntity } from "../../shared/model/entity/StoreEntity";

@Injectable({
    providedIn: 'root'
})
export class StoreService {

    constructor(private apiService: ApiService) {
    }

    async FindById(StoreCod: string) {
        let url: string = `${AppSetting.API}/api/v1/store/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { StoreCod: StoreCod });

        return RespuestaWS;
    }

    async FindAll(Query: string, Page: number) {
        let url: string = `${AppSetting.API}/api/v1/store/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async FindAllList() {
        let url: string = `${AppSetting.API}/api/v1/store/findAllList`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }

    async FindUbigeo(UbigeoCod: string) {
        let url: string = `${AppSetting.API}/api/v1/store/findUbigeo`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { UbigeoCod: UbigeoCod });

        return RespuestaWS;
    }

    async FindStoreInfo(StoreCod: string) {
        let url: string = `${AppSetting.API}/api/v1/store/findStoreInfo`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { StoreCod: StoreCod });

        return RespuestaWS;
    }

    async InitializeStoreAutomation(store: StoreEntity) {
        let url: string = `${AppSetting.API}/api/v1/store/initializeStoreAutomation`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, store);

        return RespuestaWS;
    }

    async Save(store: StoreEntity) {
        let url: string = `${AppSetting.API}/api/v1/store/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, store);

        return RespuestaWS;
    }

}
