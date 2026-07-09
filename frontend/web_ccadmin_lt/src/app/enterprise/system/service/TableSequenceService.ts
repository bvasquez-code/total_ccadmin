import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "src/app/enterprise/compartido/service/api.service";
import { ResponseWsDto } from "src/app/enterprise/shared/model/dto/ResponseWsDto";
import { TableSequenceEntity } from "src/app/enterprise/shared/model/entity/TableSequenceEntity";

@Injectable({
    providedIn: 'root'
})
export class TableSequenceService {

    constructor(private apiService: ApiService) { }

    async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/tableSequence/findAll`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecuteGetService(url, { Query: Query, Page: Page });

        return RespuestaWS;
    }

    async findDataForm(SequenceTrx: number | null = null): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/tableSequence/findDataForm`;
        let RespuestaWS: ResponseWsDto;
        const request: any = {};

        if (SequenceTrx !== null) request.SequenceTrx = SequenceTrx;

        RespuestaWS = await this.apiService.ExecuteGetService(url, request);

        return RespuestaWS;
    }

    async save(tableSequence: TableSequenceEntity): Promise<ResponseWsDto> {
        let url: string = `${AppSetting.API}/api/v1/tableSequence/save`;
        let RespuestaWS: ResponseWsDto;

        RespuestaWS = await this.apiService.ExecutePostService(url, tableSequence);

        return RespuestaWS;
    }
}
