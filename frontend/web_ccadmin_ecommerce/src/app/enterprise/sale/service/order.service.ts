import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';

@Injectable({ providedIn: 'root' })
export class OrderService {

  public constructor(private apiService: ApiService) {
  }

  public findMyOrders(Page: number): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/sale/findMyOrders`;
    return this.apiService.ExecuteGetService(url, { Page });
  }
}
