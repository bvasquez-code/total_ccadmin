import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { StoreService } from './store.service';

@Injectable({ providedIn: 'root' })
export class CurrentStoreService {
  private readonly cacheKey = 'CurrentStoreName';
  private currentRequest: Promise<string> | null = null;
  private currentRequestKey = '';

  constructor(
    private storeService: StoreService,
    private dataSesionService: DataSesionService,
    private toastrService: ToastrService
  ) {
  }

  getCurrentStoreName(): Promise<string> {
    const storeCode = this.dataSesionService.getSessionStorageDto().StoreCod?.trim() || '';
    if (!this.dataSesionService.SessionExists() || !storeCode) return Promise.resolve('');

    const sessionIdentity = this.getSessionIdentity();
    const sessionToken = this.dataSesionService.GetToken();
    const cachedName = this.readCachedName(sessionIdentity);
    if (cachedName !== null) return Promise.resolve(cachedName);

    const requestKey = JSON.stringify([sessionIdentity, sessionToken]);
    if (this.currentRequest && this.currentRequestKey === requestKey) return this.currentRequest;

    const request = this.loadStoreName(storeCode, sessionIdentity, sessionToken).finally(() => {
      if (this.currentRequest === request) this.currentRequest = null;
    });
    this.currentRequestKey = requestKey;
    this.currentRequest = request;
    return request;
  }

  private async loadStoreName(storeCode: string, sessionIdentity: string, sessionToken: string): Promise<string> {
    let storeName = '';
    try {
      const response = await this.storeService.FindById(storeCode);
      if (response.ErrorStatus) {
        throw new Error(response.Message || 'No se pudo cargar el nombre de la tienda actual');
      }
      storeName = response.Data?.Name?.trim() || '';
    } catch (error) {
      if (this.isCurrentSession(sessionIdentity, sessionToken)) {
        this.toastrService.error(error instanceof Error
          ? error.message
          : 'No se pudo cargar el nombre de la tienda actual');
      }
    }

    // Una respuesta anterior no debe modificar los datos de la nueva sesión.
    if (!this.isCurrentSession(sessionIdentity, sessionToken)) return '';

    // SaveSession, ClearSession y la sincronización entre pestañas limpian sessionStorage.
    // También se conserva el resultado vacío para evitar reintentos automáticos.
    sessionStorage.setItem(this.cacheKey, JSON.stringify({ sessionIdentity, storeName }));
    return storeName;
  }

  private getSessionIdentity(): string {
    const session = this.dataSesionService.getSessionStorageDto();
    return JSON.stringify([session.SessionID, session.UserCod, session.StoreCod]);
  }

  private isCurrentSession(sessionIdentity: string, sessionToken: string): boolean {
    return this.getSessionIdentity() === sessionIdentity && this.dataSesionService.GetToken() === sessionToken;
  }

  private readCachedName(sessionIdentity: string): string | null {
    try {
      const cached = JSON.parse(sessionStorage.getItem(this.cacheKey) || 'null');
      return cached?.sessionIdentity === sessionIdentity && typeof cached.storeName === 'string'
        ? cached.storeName
        : null;
    } catch {
      return null;
    }
  }
}
