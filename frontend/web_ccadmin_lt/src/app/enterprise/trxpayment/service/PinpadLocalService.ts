import { Injectable } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { ApiService } from '../../compartido/service/api.service';
import { PinpadBrowserInstructionsDto, PinpadSignedMessageDto } from '../model/dto/PinpadBrowserInstructionsDto';
import { DataSesionService } from '../../compartido/service/datasesion.service';

@Injectable({ providedIn: 'root' })
export class PinpadLocalService {
  constructor(private apiService: ApiService, private dataSesionService: DataSesionService) {}

  async process(instructions: PinpadBrowserInstructionsDto): Promise<PinpadSignedMessageDto> {
    const deadline = Date.now() + instructions.waitSeconds * 1000;
    const token = await this.login(instructions.loginUrl, instructions.loginCommand);
    let result: PinpadSignedMessageDto;
    try {
      result = await this.execute(instructions.registerUrl, token);
    } catch {
      // The agent may already have received the charge. Recover the original reference with STATUS.
      result = await this.execute(instructions.statusUrl, token);
    }
    while (['CREATED', 'PROCESSING'].includes(this.status(result)) && Date.now() < deadline) {
      await new Promise(resolve => setTimeout(resolve, Math.min(instructions.pollMillis, Math.max(0, deadline - Date.now()))));
      result = await this.execute(instructions.statusUrl, token);
    }
    return result;
  }

  private validateEndpoint(url: string): void {
    const endpoint = new URL(url);
    if (!['http:', 'https:'].includes(endpoint.protocol) || !['127.0.0.1', 'localhost'].includes(endpoint.hostname)
        || endpoint.port === '6669' || endpoint.username || endpoint.password) {
      throw new Error('La URL del agente local no es valida.');
    }
  }

  private async login(url: string, command: PinpadSignedMessageDto): Promise<string> {
    this.validateEndpoint(url);
    const applicationToken = this.dataSesionService.GetToken();
    if (!applicationToken) throw new Error('Inicie sesion en la aplicacion web.');
    const response = await firstValueFrom(this.apiService.InvokeLocalPostService(url,
      { authorization: command, applicationToken }).pipe(timeout(15000)));
    if (!response?.success || !response.data?.accessToken) throw new Error('No se pudo iniciar sesion en el pinpad.');
    return response.data.accessToken;
  }

  async acknowledge(loginUrl: string, command: PinpadSignedMessageDto, ackUrl: string): Promise<void> {
    const token = await this.login(loginUrl, command);
    await this.execute(ackUrl, token);
  }

  private async execute(url: string, token: string): Promise<PinpadSignedMessageDto> {
    this.validateEndpoint(url);
    const response = await firstValueFrom(this.apiService.InvokeLocalPostService(url, {}, token).pipe(timeout(10000)));
    if (!response?.success || !response.data?.payload || !response.data?.signature) {
      throw new Error('El agente no devolvio un resultado firmado.');
    }
    return response.data;
  }

  status(result: PinpadSignedMessageDto): string {
    const base64 = result.payload.replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(base64), character => character.charCodeAt(0));
    // This decoding drives the UI only. The backend must verify the signature before accepting a payment.
    return JSON.parse(new TextDecoder().decode(bytes)).result?.status || 'UNKNOWN';
  }
}
