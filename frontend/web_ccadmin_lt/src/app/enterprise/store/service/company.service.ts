import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { CompanyEntity } from '../../shared/model/entity/CompanyEntity';

@Injectable({
  providedIn: 'root'
})
export class CompanyService {

  public constructor(private apiService: ApiService) {
  }

  public find(): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/company/find`;
    return this.apiService.ExecuteGetService(url, {});
  }

  public save(company: CompanyEntity): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/company/save`;
    return this.apiService.ExecutePostService(url, company);
  }
}
