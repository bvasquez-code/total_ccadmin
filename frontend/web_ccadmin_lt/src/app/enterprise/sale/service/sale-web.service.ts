import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { SaleDeliveryStatusChangeDto } from '../model/dto/SaleDeliveryStatusChangeDto';

@Injectable({
  providedIn: 'root'
})
export class SaleWebService {

  constructor(private apiService: ApiService) {}

  async findAll(
    query: string,
    page: number,
    deliveryTypeCod: string,
    deliveryStatus: string
  ): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/saleWeb/findAll`;
    return await this.apiService.ExecuteGetService(url, {
      Query: query,
      Page: page,
      DeliveryTypeCod: deliveryTypeCod,
      DeliveryStatus: deliveryStatus
    });
  }

  async changeDeliveryStatus(request: SaleDeliveryStatusChangeDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/saleWeb/changeDeliveryStatus`;
    return await this.apiService.ExecutePostService(url, request);
  }
}
