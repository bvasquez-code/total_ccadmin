import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';
import { ClientProfileUpdateDto } from '../model/dto/ClientProfileUpdateDto';

@Injectable({ providedIn: 'root' })
export class ClientProfileService {

  public constructor(private apiService: ApiService) {
  }

  public find(): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAccount/findProfile`;
    return this.apiService.ExecuteGetService(url, {});
  }

  public update(request: ClientProfileUpdateDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAccount/updateProfile`;
    return this.apiService.ExecutePostService(url, request);
  }
}
