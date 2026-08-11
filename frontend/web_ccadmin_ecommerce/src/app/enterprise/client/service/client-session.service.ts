import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AppSetting } from '../../../config/app.setting';
import { StorageConstants } from '../../shared/model/constants/StorageConstants';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';
import { ClientLoginDto } from '../model/dto/ClientLoginDto';
import { ClientRegisterDto } from '../model/dto/ClientRegisterDto';
import { ClientSessionDto } from '../model/dto/ClientSessionDto';

@Injectable({ providedIn: 'root' })
export class ClientSessionService {
  private sessionSubject = new BehaviorSubject<ClientSessionDto | null>(this.readSession());

  public readonly Session$: Observable<ClientSessionDto | null> = this.sessionSubject.asObservable();

  public constructor(private apiService: ApiService) {
  }

  public getCurrent(): ClientSessionDto | null {
    return this.sessionSubject.value;
  }

  public isAuthenticated(): boolean {
    return !!this.sessionSubject.value?.Token;
  }

  public async login(request: ClientLoginDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAccount/login`;
    const response = await this.apiService.ExecutePostService(url, request);
    this.persistSession(response);
    return response;
  }

  public async register(request: ClientRegisterDto): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/delivery/clientAccount/register`;
    const response = await this.apiService.ExecutePostService(url, request);
    this.persistSession(response);
    return response;
  }

  public logout(): void {
    localStorage.removeItem(StorageConstants.CLIENT_SESSION);
    this.sessionSubject.next(null);
  }

  private readSession(): ClientSessionDto | null {
    const value = localStorage.getItem(StorageConstants.CLIENT_SESSION);
    if (!value) return null;

    try {
      const session = Object.assign(new ClientSessionDto(), JSON.parse(value));
      return session.Token ? session : null;
    } catch {
      localStorage.removeItem(StorageConstants.CLIENT_SESSION);
      return null;
    }
  }

  private persistSession(response: ResponseWsDto): void {
    if (response.ErrorStatus || !response.Data?.Token) return;

    const session = Object.assign(new ClientSessionDto(), response.Data);
    localStorage.setItem(StorageConstants.CLIENT_SESSION, JSON.stringify(session));
    this.sessionSubject.next(session);
  }
}
