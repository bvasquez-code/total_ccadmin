import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { CounterfoilRegisterDto } from "../model/dto/CounterfoilRegisterDto";
import { CounterfoilRegisterListDto } from "../model/dto/CounterfoilRegisterListDto";

@Injectable({
    providedIn: 'root'
})
export class CounterfoilService {

    constructor(private apiService: ApiService) { }

    async findById(CounterfoilCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { CounterfoilCod: CounterfoilCod });

        return RespuestaWS;
    }

    async findAll(Query: string, Page: number, StoreCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page, StoreCod: StoreCod });

        return RespuestaWS;
    }

    async findActives(): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/findActives`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }

    async formData(CounterfoilCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/formData`;
        let RespuestaWS: ResponseWsDto;

        // CounterfoilCod es opcional en backend
        RespuestaWS = await this.apiService.ExecuteGetService(url, { CounterfoilCod: CounterfoilCod });

        return RespuestaWS;
    }

    async save(counterfoil: CounterfoilRegisterDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, counterfoil);

        return RespuestaWS;
    }

    async saveAll(counterfoilRegisterList: CounterfoilRegisterListDto): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/saveAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, counterfoilRegisterList);

        return RespuestaWS;
    }

    async enable(CounterfoilCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/enable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, { CounterfoilCod: CounterfoilCod });

        return RespuestaWS;
    }

    async disable(CounterfoilCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/disable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, { CounterfoilCod: CounterfoilCod });

        return RespuestaWS;
    }

    async attachStore(CounterfoilCod: string, StoreCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/attachStore`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, { CounterfoilCod: CounterfoilCod, StoreCod: StoreCod });

        return RespuestaWS;
    }

    async detachStore(CounterfoilCod: string, StoreCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/detachStore`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, { CounterfoilCod: CounterfoilCod, StoreCod: StoreCod });

        return RespuestaWS;
    }

    async existsSeries(Series: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/counterfoil/existsSeries`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Series: Series });

        return RespuestaWS;
    }
}
