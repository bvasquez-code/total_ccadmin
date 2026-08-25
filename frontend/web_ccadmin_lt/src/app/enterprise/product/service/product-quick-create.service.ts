import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';

@Injectable({ providedIn: 'root' })
export class ProductQuickCreateService {
  private readonly baseUrl = `${AppSetting.API}/api/v1/product`;

  constructor(private apiService: ApiService) {}

  analyzeImages(
    frontImage: Blob,
    sideImage: Blob,
    barcodeImage: Blob
  ): Promise<ResponseWsDto> {
    const formData = new FormData();
    formData.append('frontImage', frontImage, 'producto-frontal.jpg');
    formData.append('sideImage', sideImage, 'producto-lateral.jpg');
    formData.append('barcodeImage', barcodeImage, 'producto-codigo-barras.jpg');
    return this.apiService.ExecutePostFormDataService(
      `${this.baseUrl}/analyzeQuickCreateImage`, formData
    );
  }
}
