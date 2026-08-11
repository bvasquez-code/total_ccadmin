import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';
import { DeliveryCoverageRequestDto } from '../model/dto/DeliveryCoverageRequestDto';
import { ClientAddressEntity } from '../model/entity/ClientAddressEntity';

@Injectable({ providedIn: 'root' })
export class ClientAddressService {

  public constructor(private apiService: ApiService) {
  }

  public findAll(): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAddress/findAll`;
    return this.apiService.ExecuteGetService(url, {});
  }

  public save(request: ClientAddressEntity): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAddress/save`;
    return this.apiService.ExecutePostService(url, request);
  }

  public validateCoverage(request: DeliveryCoverageRequestDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAddress/validateCoverage`;
    return this.apiService.ExecutePostService(url, request);
  }
}
