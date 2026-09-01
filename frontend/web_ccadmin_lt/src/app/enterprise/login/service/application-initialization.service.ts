import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../compartido/service/api.service';

export interface ApplicationInitializationStatusDto {
  Required: boolean;
  CompanyPending: boolean;
  StorePending: boolean;
  DefaultStoreCod: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApplicationInitializationService {

  constructor(private apiService: ApiService) {
  }

  async findStatus(): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/security/findApplicationInitializationStatus`;
    return this.apiService.ExecuteGetService(url, {});
  }
}
