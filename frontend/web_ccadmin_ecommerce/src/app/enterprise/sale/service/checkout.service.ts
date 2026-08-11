import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';
import { CheckoutRegisterDto } from '../model/dto/CheckoutRegisterDto';
import { PresaleRegisterDto } from '../model/dto/PresaleRegisterDto';
import { SalePaymentDeliveryRegisterDto } from '../model/dto/SalePaymentDeliveryRegisterDto';
import { SaleDeliveryAccessRequestDto } from '../model/dto/SaleDeliveryAccessRequestDto';

@Injectable({ providedIn: 'root' })
export class CheckoutService {

  public constructor(private apiService: ApiService) {
  }

  public createCode(StoreCod: string): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/presale/createCode`;
    return this.apiService.ExecuteGetService(url, { StoreCod });
  }

  public save(request: CheckoutRegisterDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/presale/save`;
    return this.apiService.ExecutePostService(url, request);
  }

  public confirm(request: PresaleRegisterDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/presale/confirm`;
    return this.apiService.ExecutePostService(url, request);
  }

  public findSaleData(OrderToken: string): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/sale/findDataForm`;
    const request = new SaleDeliveryAccessRequestDto();
    request.OrderToken = OrderToken;
    return this.apiService.ExecutePostService(url, request);
  }

  public addPayment(request: SalePaymentDeliveryRegisterDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/sale/addPayment`;
    return this.apiService.ExecutePostService(url, request);
  }
}
