import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';
import { ProductSearchDto } from '../model/dto/ProductSearchDto';

@Injectable({ providedIn: 'root' })
export class ProductDeliverySearchService {

  public constructor(private apiService: ApiService) {
  }

  public query(request: ProductSearchDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/productSearch/query`;
    return this.apiService.ExecutePostService(url, request);
  }

  public findAvailability(ProductCod: string, StoreCod: string): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/productSearch/findAvailability`;
    return this.apiService.ExecuteGetService(url, { ProductCod, StoreCod });
  }
}
