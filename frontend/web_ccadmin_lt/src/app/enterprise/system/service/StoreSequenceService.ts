import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { StoreSequenceEntity } from "src/app/enterprise/shared/model/entity/StoreSequenceEntity";

@Injectable({
    providedIn: 'root'
})
export class StoreSequenceService {

    constructor(private apiService: ApiService) { }

    async findById(StoreCod: string, PeriodId: number, SequenceTableType: string): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/storeSequence/findById`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {
            StoreCod: StoreCod,
            PeriodId: PeriodId,
            SequenceTableType: SequenceTableType
        });

        return RespuestaWS;
    }

    async findAll(Query: string, Page: number, StoreCod: string = ""): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/storeSequence/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, {
            Query: Query,
            Page: Page,
            StoreCod: StoreCod
        });

        return RespuestaWS;
    }

    async findDataForm(StoreCod: string = "", PeriodId: number | null = null, SequenceTableType: string = ""): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/storeSequence/findDataForm`;
        let RespuestaWS: ResponseWsDto;
        const request: any = {};

        if (StoreCod) request.StoreCod = StoreCod;
        if (PeriodId !== null) request.PeriodId = PeriodId;
        if (SequenceTableType) request.SequenceTableType = SequenceTableType;

        RespuestaWS = await this.apiService.ExecuteGetService(url, request);

        return RespuestaWS;
    }

    async save(storeSequence: StoreSequenceEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/storeSequence/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, storeSequence);

        return RespuestaWS;
    }
}
