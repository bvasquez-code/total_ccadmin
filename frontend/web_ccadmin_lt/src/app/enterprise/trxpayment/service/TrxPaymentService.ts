import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ICrudService } from "../../shared/interface/ICrudService";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { SearchDto } from "../../shared/model/dto/SearchDto";
import { TrxPaymentEntity } from "../model/entity/TrxPaymentEntity";
import { PinpadLocalService } from "./PinpadLocalService";
import { PinpadBrowserInstructionsDto } from "../model/dto/PinpadBrowserInstructionsDto";

@Injectable({
    providedIn: 'root'
})
export class TrxPaymentService implements ICrudService<TrxPaymentEntity,number>{
    private readonly registrationAttempted = new WeakSet<TrxPaymentEntity>();

    constructor(private apiService: ApiService, private pinpadLocalService: PinpadLocalService) {
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

        try {
            if (TrxPayment.TypeMovement === 'I' && ['POS', 'PINPAD'].includes(TrxPayment.PaymentPlatform.toUpperCase())) {
                const status = TrxPayment.PinpadResult ? this.pinpadLocalService.status(TrxPayment.PinpadResult) : '';
                if (!['APPROVED', 'READ'].includes(status)) {
                    const prepared = await this.apiService.ExecutePostService(
                        `${AppSetting.API}/api/v1/TrxPayment/preparePinpad`, TrxPayment);
                    if (!prepared || prepared.ErrorStatus !== false) return this.pinpadError(TrxPayment, prepared);
                    TrxPayment.PinpadResult = await this.pinpadLocalService.process(prepared.Data as PinpadBrowserInstructionsDto,
                        () => this.registrationAttempted.add(TrxPayment));
                }
            }
            RespuestaWS = this.normalize(await this.apiService.ExecutePostService(url,TrxPayment));
        } catch {
            return this.pinpadError(TrxPayment, null);
        }

        if (!RespuestaWS.ErrorStatus && RespuestaWS.Data?.PinpadAckCommand && RespuestaWS.Data?.PinpadAckUrl && RespuestaWS.Data?.PinpadLoginUrl) {
            try {
                await this.pinpadLocalService.acknowledge(RespuestaWS.Data.PinpadLoginUrl,
                    RespuestaWS.Data.PinpadAckCommand, RespuestaWS.Data.PinpadAckUrl);
            } catch {
                // The backend has already committed the approval. A retry of save returns a fresh ACK command.
            }
        }
        return RespuestaWS;
    }

    private pinpadError(payment: TrxPaymentEntity, response: ResponseWsDto | null): ResponseWsDto {
        const result = this.normalize(response);
        if (payment.TypeMovement === 'I' && ['POS', 'PINPAD'].includes(payment.PaymentPlatform.toUpperCase())
            && !this.registrationAttempted.has(payment) && !payment.PinpadResult
            && typeof result.Data?.CanStartNewPayment !== 'boolean') {
            result.Data = { PinpadPaymentId: payment.PinpadPaymentId, PaymentStatus: 'NOT_SENT', CanStartNewPayment: true };
            if (!response) result.Message = 'No se pudo conectar al servicio pinpad. El cobro no fue enviado; puede elegir otro medio de pago.';
        }
        return result;
    }

    private normalize(RespuestaWS: ResponseWsDto | null): ResponseWsDto {
        if (!RespuestaWS || typeof RespuestaWS.ErrorStatus !== 'boolean') {
            RespuestaWS = new ResponseWsDto();
            RespuestaWS.ErrorStatus = true;
            RespuestaWS.Message = 'No se pudo confirmar el pago. Compruebe el agente de esta PC y el permiso de red local; reintente con la misma referencia.';
        }

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

    async FindDocumentsByTrxPaymentId(TrxPaymentId: number): Promise<ResponseWsDto> {
        const url: string = `${AppSetting.API}/api/v1/TrxPayment/findDocumentsByTrxPaymentId`;
        return this.apiService.ExecuteGetService(url, { TrxPaymentId: TrxPaymentId });
    }
}
