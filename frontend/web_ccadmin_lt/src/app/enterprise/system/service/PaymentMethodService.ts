import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { PaymentMethodEntity } from "src/app/enterprise/shared/model/entity/PaymentMethodEntity";

@Injectable({
    providedIn: 'root'
})
export class PaymentMethodService {

    constructor(private apiService: ApiService) { }

    async findById(PaymentMethodCod: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { PaymentMethodCod: PaymentMethodCod });

        return RespuestaWS;
    }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async findActives(): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/findActives`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {});

        return RespuestaWS;
    }

    async findDataForm(PaymentMethodCod: string = ""): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/findDataForm`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { PaymentMethodCod: PaymentMethodCod });

        return RespuestaWS;
    }

    async save(paymentMethod: PaymentMethodEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, paymentMethod);

        return RespuestaWS;
    }

    async saveAll(paymentMethodList: PaymentMethodEntity[]): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/saveAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, paymentMethodList);

        return RespuestaWS;
    }

    async enable(paymentMethod: PaymentMethodEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/enable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, paymentMethod);

        return RespuestaWS;
    }

    async disable(paymentMethod: PaymentMethodEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/paymentMethod/disable`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, paymentMethod);

        return RespuestaWS;
    }
}
