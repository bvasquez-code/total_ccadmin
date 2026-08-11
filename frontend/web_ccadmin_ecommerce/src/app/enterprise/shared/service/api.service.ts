import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ClientSessionDto } from '../../client/model/dto/ClientSessionDto';
import { StorageConstants } from '../model/constants/StorageConstants';
import { ResponseWsDto } from '../model/dto/ResponseWsDto';

@Injectable({ providedIn: 'root' })
export class ApiService {

  public constructor(private httpClient: HttpClient) {
  }

  public async ExecuteGetService(URL: string, Request: Record<string, unknown>): Promise<ResponseWsDto> {
    let parameters = new HttpParams();
    Object.entries(Request).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        parameters = parameters.set(key, String(value));
      }
    });

    try {
      return await firstValueFrom(this.httpClient.get<ResponseWsDto>(URL, {
        headers: this.createHeaders(),
        params: parameters
      }));
    } catch (error: any) {
      return ResponseWsDto.fromError(error);
    }
  }

  public async ExecutePostService(URL: string, Request: unknown): Promise<ResponseWsDto> {
    try {
      return await firstValueFrom(this.httpClient.post<ResponseWsDto>(URL, Request, {
        headers: this.createHeaders()
      }));
    } catch (error: any) {
      return ResponseWsDto.fromError(error);
    }
  }

  private createHeaders(): HttpHeaders {
    let headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const session = this.readClientSession();
    if (session?.Token) {
      headers = headers.set('Authorization', session.Token);
    }
    return headers;
  }

  private readClientSession(): ClientSessionDto | null {
    const value = localStorage.getItem(StorageConstants.CLIENT_SESSION);
    if (!value) return null;

    try {
      return Object.assign(new ClientSessionDto(), JSON.parse(value));
    } catch {
      localStorage.removeItem(StorageConstants.CLIENT_SESSION);
      return null;
    }
  }
}
