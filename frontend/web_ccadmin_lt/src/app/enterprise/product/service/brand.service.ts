import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { ProductRegisterDto } from "../model/dto/ProductRegisterDto";
import { BrandEntity } from "../model/entity/BrandEntity";
import { BrandRegisterMassiveDto } from "../model/dto/BrandRegisterMassiveDto";

@Injectable({
    providedIn: 'root'
})

export class BrandService {
    constructor(private apiService: ApiService) {
    }

    async FindAll(Query: string, Page: number) {
        let url: string = `${AppSetting.API}/api/v1/brand/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async FindDataForm(BrandCod: string) {
        let url: string = `${AppSetting.API}/api/v1/brand/findDataForm`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { BrandCod: BrandCod });

        return RespuestaWS;
    }

    async Save(Brand: BrandEntity) {
        let url: string = `${AppSetting.API}/api/v1/brand/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, Brand);

        return RespuestaWS;
    }

    async SaveAll(Brand: BrandRegisterMassiveDto) {
        let url: string = `${AppSetting.API}/api/v1/brand/saveAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, Brand);

        return RespuestaWS;
    }
}