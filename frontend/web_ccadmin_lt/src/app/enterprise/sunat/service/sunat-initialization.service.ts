import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';

@Injectable({ providedIn: 'root' })
export class SunatInitializationService {
  private readonly baseUrl = `${AppSetting.API}/api/v1/sunatInitialization`;

  constructor(private apiService: ApiService) {}

  findDataForm(): Promise<ResponseWsDto> {
    return this.apiService.ExecuteGetService(`${this.baseUrl}/findDataForm`, {});
  }

  configure(configuration: object, certificate?: File): Promise<ResponseWsDto> {
    const request = new FormData();
    request.append('configuration', JSON.stringify(configuration));
    if (certificate) request.append('certificate', certificate);
    return this.apiService.ExecutePostFormDataService(`${this.baseUrl}/configure`, request);
  }
}
