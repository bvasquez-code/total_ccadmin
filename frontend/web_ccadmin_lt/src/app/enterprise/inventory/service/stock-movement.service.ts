import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { StockMovementKind, StockMovementRegister } from '../model/StockMovementModels';

@Injectable({ providedIn: 'root' })
export class StockMovementService {
  constructor(private apiService: ApiService) {}

  findAll(kind: StockMovementKind, request: any): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.url(kind)}/findAll`, request);
  }

  findDataForm(kind: StockMovementKind, code: string = ''): Promise<ResponseWsDto> {
    return this.apiService.ExecuteGetService(`${this.url(kind)}/findDataForm`, { code });
  }

  findById(kind: StockMovementKind, code: string): Promise<ResponseWsDto> {
    return this.apiService.ExecuteGetService(`${this.url(kind)}/findById`, { code });
  }

  save(kind: StockMovementKind, request: StockMovementRegister): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.url(kind)}/save`, request);
  }

  confirm(kind: StockMovementKind, code: string): Promise<ResponseWsDto> {
    return this.action(kind, 'confirm', code);
  }

  resolve(kind: StockMovementKind, request: any): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.url(kind)}/resolve`, request);
  }

  reject(kind: StockMovementKind, code: string, observation: string): Promise<ResponseWsDto> {
    return this.action(kind, 'reject', code, observation);
  }

  cancel(kind: StockMovementKind, code: string, observation: string): Promise<ResponseWsDto> {
    return this.action(kind, 'cancel', code, observation);
  }

  private action(kind: StockMovementKind, action: string, Code: string, Observation: string = ''): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${this.url(kind)}/${action}`, { Code, Observation });
  }

  private url(kind: StockMovementKind): string {
    return `${AppSetting.API}/api/v1/${kind === 'entry' ? 'stockEntry' : 'stockExit'}`;
  }
}
