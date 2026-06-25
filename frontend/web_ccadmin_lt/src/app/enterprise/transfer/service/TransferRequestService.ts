import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { TransferDispatchDto } from "../model/dto/TransferDispatchDto";
import { TransferReceiveDto } from "../model/dto/TransferReceiveDto";
import { TransferRegisterBundleDto } from "../model/dto/TransferRegisterBundleDto";
import { TransferSearchDto } from "../model/dto/TransferSearchDto";
import { TransferRequestRegisterBundleDto } from "../model/dto/TransferRequestRegisterBundleDto";

@Injectable({
    providedIn: 'root'
})
export class TransferRequestService {

    constructor(private apiService: ApiService) {
    }

    async FindAll(Search: TransferSearchDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/findAll`;
        return await this.apiService.ExecutePostService(url, Search);
    }

    async FindById(TransferCod: string): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/findById`;
        return await this.apiService.ExecuteGetService(url, { TransferCod: TransferCod });
    }

    async RegisterBundle(Entity: TransferRequestRegisterBundleDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/register-bundle`;
        return await this.apiService.ExecutePostService(url, Entity);
    }

    async FindDataForm(TransferReqCod: string): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/findDataForm`;
        return await this.apiService.ExecuteGetService(url, { TransferReqCod: TransferReqCod });
    }

    async FindDataPrint(TransferReqCod: string): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/findDataPrint`;
        return await this.apiService.ExecuteGetService(url, { TransferReqCod: TransferReqCod });
    }

    async DispatchTransfer(Request: TransferDispatchDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/dispatch`;
        return await this.apiService.ExecutePostService(url, Request);
    }

    async ReceiveTransfer(Request: TransferReceiveDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/receive`;
        return await this.apiService.ExecutePostService(url, Request);
    }

    async RejectTransfer(Request: TransferReceiveDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/reject`;
        return await this.apiService.ExecutePostService(url, Request);
    }

    async CancelTransfer(Request: TransferReceiveDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/cancel`;
        return await this.apiService.ExecutePostService(url, Request);
    }

    async ApprovedTransfer(Request: TransferReceiveDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/approved`;
        return await this.apiService.ExecutePostService(url, Request);
    }

    async CreateCode(StoreCod: string): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/createCode`;
        return await this.apiService.ExecuteGetService(url, { storeCod: StoreCod });
    }

    async ConfirmedTransfer(Request: TransferReceiveDto): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/transfers-request/confirmed`;
        return await this.apiService.ExecutePostService(url, Request);
    }
}
