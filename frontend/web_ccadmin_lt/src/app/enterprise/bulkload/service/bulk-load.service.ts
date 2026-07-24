import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { BulkLoadParsedRequest } from '../model/BulkLoadModels';

@Injectable({ providedIn: 'root' })
export class BulkLoadService {
  private readonly baseUrl = `${AppSetting.API}/api/v1/bulkLoad`;

  constructor(private apiService: ApiService) {}

  findAll(request: unknown): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.baseUrl}/findAll`, request);
  }

  findById(code: string): Promise<ResponseWsDto> {
    return this.apiService.ExecuteGetService(`${this.baseUrl}/findById`, { code });
  }

  findDetails(request: unknown): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.baseUrl}/findDetails`, request);
  }

  saveParsed(request: BulkLoadParsedRequest): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.baseUrl}/saveParsed`, request);
  }

  confirm(code: string): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.baseUrl}/confirm`, { Code: code });
  }

  cancel(code: string): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.baseUrl}/cancel`, { Code: code });
  }
}
