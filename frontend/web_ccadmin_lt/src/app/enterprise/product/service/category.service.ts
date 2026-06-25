import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { ProductRegisterDto } from "../model/dto/ProductRegisterDto";
import { CategoryEntity } from "../model/entity/CategoryEntity";
import { CategoryRegisterMassiveDto } from "../model/dto/CategoryRegisterMassiveDto";

@Injectable({
    providedIn: 'root'
})

export class CategoryService {
    constructor(private apiService: ApiService) {
    }

    async FindAll(Query: string, Page: number) {
        let url: string = `${AppSetting.API}/api/v1/category/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async FindDataForm(CategoryCod: string) {
        let url: string = `${AppSetting.API}/api/v1/category/findDataForm`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { CategoryCod: CategoryCod });

        return RespuestaWS;
    }

    async Save(Category: CategoryEntity) {
        let url: string = `${AppSetting.API}/api/v1/category/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, Category);

        return RespuestaWS;
    }

    async SaveAll(Category: CategoryRegisterMassiveDto) {
        let url: string = `${AppSetting.API}/api/v1/category/saveAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, Category);

        return RespuestaWS;
    }

    async FindDataFormMassive() {
        let url: string = `${AppSetting.API}/api/v1/category/findDataFormMassive`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }
}