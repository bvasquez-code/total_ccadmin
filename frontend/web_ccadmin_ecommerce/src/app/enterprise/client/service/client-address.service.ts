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

  public findDepartments(): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAddress/findDepartments`;
    return this.apiService.ExecuteGetService(url, {});
  }

  public findProvinces(departmentCod: string): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAddress/findProvinces`;
    return this.apiService.ExecuteGetService(url, { DepartmentCod: departmentCod });
  }

  public findDistricts(provinceCod: string): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAddress/findDistricts`;
    return this.apiService.ExecuteGetService(url, { ProvinceCod: provinceCod });
  }

  public validateCoverage(request: DeliveryCoverageRequestDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAddress/validateCoverage`;
    return this.apiService.ExecutePostService(url, request);
  }
}
