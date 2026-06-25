import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ICrudService } from "../../shared/interface/ICrudService";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { TrxPaymentEntity } from "../model/entity/TrxPaymentEntity";

@Injectable({
    providedIn: 'root'
})
export class TrxPaymentService implements ICrudService<TrxPaymentEntity,number>{

    constructor(private apiService: ApiService) {
    }
    
    async FindById(Id: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/TrxPayment/findById`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ TrxPaymentId: Id });

        return RespuestaWS;
    }

    async FindAll(Search: SearchDto): Promise<ResponseWsDto> {
        return this.findAll(Search.Query, Search.Page);
    }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/TrxPayment/findAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ Query: Query, Page: Page });

        return RespuestaWS;
    }

    async FindByTransactionId(TransactionId: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/TrxPayment/findByTransactionId`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ TransactionId: TransactionId });

        return RespuestaWS;
    }

    async Save(TrxPayment: TrxPaymentEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/TrxPayment/save`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,TrxPayment);

        return RespuestaWS;
    }

    async SaveAll(EntityList: TrxPaymentEntity[]): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/TrxPayment/saveAll`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url,EntityList);

        return RespuestaWS;
    }

    FindAllById(IdList: number[]): Promise<ResponseWsDto> {
        throw new Error("Method not implemented.");
    }


    async FindDataForm(): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/TrxPayment/findDataForm`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{});

        return RespuestaWS;
    }

    async FindDataFormView(TrxPaymentId: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/TrxPayment/findDataFormView`;
        let RespuestaWS : ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url,{ TrxPaymentId: TrxPaymentId });

        return RespuestaWS;
    }
}
