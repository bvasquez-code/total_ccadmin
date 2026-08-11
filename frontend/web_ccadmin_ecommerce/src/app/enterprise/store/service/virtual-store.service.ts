import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { ApiService } from '../../shared/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { StoreLocationRequestDto } from '../model/dto/StoreLocationRequestDto';

@Injectable({ providedIn: 'root' })
export class VirtualStoreService {

  public constructor(private apiService: ApiService) {
  }

  public resolveByIp(): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/store/resolveByIp`;
    return this.apiService.ExecuteGetService(url, {});
  }

  public resolveLocation(request: StoreLocationRequestDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/store/resolveLocation`;
    return this.apiService.ExecutePostService(url, request);
  }
}
