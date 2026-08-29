import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { StoreEntity } from "../../shared/model/entity/StoreEntity";
import { StoreVirtualConfigRegisterDto } from "../model/dto/StoreVirtualConfigRegisterDto";

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

    async FindCompanies() {
        const url: string = `${AppSetting.API}/api/v1/store/findCompanies`;
        return this.apiService.ExecuteGetService(url, {});
    }

    async FindCountries() {
        const url: string = `${AppSetting.API}/api/v1/store/findCountries`;
        return this.apiService.ExecuteGetService(url, {});
    }

    async FindDepartments() {
        const url: string = `${AppSetting.API}/api/v1/store/findDepartments`;
        return this.apiService.ExecuteGetService(url, {});
    }

    async FindProvinces(DepartmentCod: string) {
        const url: string = `${AppSetting.API}/api/v1/store/findProvinces`;
        return this.apiService.ExecuteGetService(url, { DepartmentCod: DepartmentCod });
    }

    async FindDistricts(ProvinceCod: string) {
        const url: string = `${AppSetting.API}/api/v1/store/findDistricts`;
        return this.apiService.ExecuteGetService(url, { ProvinceCod: ProvinceCod });
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

    async FindVirtualConfig(StoreCod: string) {
        let url: string = `${AppSetting.API}/api/v1/store/findVirtualConfig`;
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

    async SaveVirtualConfig(register: StoreVirtualConfigRegisterDto) {
        let url: string = `${AppSetting.API}/api/v1/store/saveVirtualConfig`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, register);

        return RespuestaWS;
    }

}
