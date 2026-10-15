import { of, throwError } from 'rxjs';
import { ApiService } from '../../compartido/service/api.service';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { PinpadBrowserInstructionsDto } from '../model/dto/PinpadBrowserInstructionsDto';
import { PinpadLocalService } from './PinpadLocalService';

describe('PinpadLocalService fase de envio', () => {
  const instructions: PinpadBrowserInstructionsDto = {
    loginUrl: 'http://127.0.0.1:8094/pinpad/login', registerUrl: 'http://127.0.0.1:8094/pinpad/payment/register',
    statusUrl: 'http://127.0.0.1:8094/pinpad/payment/status', ackUrl: 'http://127.0.0.1:8094/pinpad/payment/ack',
    loginCommand: { payload: 'grant', signature: 'signature' }, waitSeconds: 0, pollMillis: 1000
  };
  let api: jasmine.SpyObj<ApiService>;
  let service: PinpadLocalService;
  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('api', ['InvokeLocalPostService']);
    const session = jasmine.createSpyObj<DataSesionService>('session', ['GetToken']);
    session.GetToken.and.returnValue('web-token'); service = new PinpadLocalService(api, session);
  });
  it('un fallo de login no intenta registrar el cobro', async () => {
    api.InvokeLocalPostService.and.returnValue(throwError(() => new Error('connection refused')));
    const attempted = jasmine.createSpy('attempted');
    await expectAsync(service.process(instructions, attempted)).toBeRejected();
    expect(attempted).not.toHaveBeenCalled();
    expect(api.InvokeLocalPostService.calls.count()).toBe(1);
  });
  it('marca el envio antes del registro y recupera la respuesta perdida con consulta', async () => {
    const attempted = jasmine.createSpy('attempted');
    const proof = { payload: btoa(JSON.stringify({ result: { status: 'APPROVED' } })), signature: 'signature' };
    api.InvokeLocalPostService.and.callFake(url => {
      if (url === instructions.loginUrl) return of({ success: true, data: { accessToken: 'local-token' } });
      expect(attempted).toHaveBeenCalledTimes(1);
      if (url === instructions.registerUrl) return throwError(() => new Error('lost response'));
      return of({ success: true, data: proof });
    });
    expect(await service.process(instructions, attempted)).toEqual(proof);
    expect(api.InvokeLocalPostService.calls.allArgs().map(args => args[0])).toEqual([
      instructions.loginUrl, instructions.registerUrl, instructions.statusUrl
    ]);
  });
});
