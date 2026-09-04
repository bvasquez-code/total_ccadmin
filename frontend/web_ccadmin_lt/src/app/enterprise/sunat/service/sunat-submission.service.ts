import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { SunatSubmissionSearch } from '../model/SunatSubmissionModels';

@Injectable({ providedIn: 'root' })
export class SunatSubmissionService {
  private readonly baseUrl = `${AppSetting.API}/api/v1/sunatSubmission`;

  constructor(private apiService: ApiService) {}

  findAll(request: SunatSubmissionSearch): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.baseUrl}/findAll`, request);
  }

  retry(sunatSubmissionCod: string): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.baseUrl}/retry`, {
      SunatSubmissionCod: sunatSubmissionCod
    });
  }
}
