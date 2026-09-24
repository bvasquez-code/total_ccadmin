import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { ApiService } from '../../shared/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { MercadoPagoConstructor, MercadoPagoRequestDto } from '../model/dto/MercadoPagoDto';

@Injectable({ providedIn: 'root' })
export class MercadoPagoService {
  private sdkPromise: Promise<MercadoPagoConstructor> | null = null;

  public constructor(private apiService: ApiService) {}

  public configuration(OrderToken: string): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${AppSetting.API}/api/v1/delivery/sale/mercadoPago/configuration`, { OrderToken });
  }

  public pay(request: MercadoPagoRequestDto): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${AppSetting.API}/api/v1/delivery/sale/mercadoPago/pay`, request);
  }

  public status(OrderToken: string): Promise<ResponseWsDto> {
    return this.apiService.ExecutePostService(`${AppSetting.API}/api/v1/delivery/sale/mercadoPago/status`, { OrderToken });
  }

  public loadSdk(): Promise<MercadoPagoConstructor> {
    const getConstructor = (): MercadoPagoConstructor | undefined =>
      (window as Window & { MercadoPago?: MercadoPagoConstructor }).MercadoPago;
    const loaded = getConstructor();
    if (loaded) return Promise.resolve(loaded);
    if (this.sdkPromise) return this.sdkPromise;
    this.sdkPromise = new Promise<MercadoPagoConstructor>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://sdk.mercadopago.com/js/v2';
      script.async = true;
      const fail = (): void => {
        script.remove();
        this.sdkPromise = null;
        reject(new Error('No se pudo cargar Mercado Pago. Revisa tu conexión e inténtalo nuevamente.'));
      };
      const timeout = window.setTimeout(fail, 20000);
      script.onload = () => {
        window.clearTimeout(timeout);
        const constructor = getConstructor();
        if (constructor) resolve(constructor); else fail();
      };
      script.onerror = () => { window.clearTimeout(timeout); fail(); };
      document.head.appendChild(script);
    });
    return this.sdkPromise;
  }
}
