import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { TrxPaymentEntity } from '../model/entity/TrxPaymentEntity';
import { PinpadLocalService } from './PinpadLocalService';
import { TrxPaymentService } from './TrxPaymentService';

describe('TrxPaymentService errores antes y despues del envio', () => {
  let api: jasmine.SpyObj<ApiService>;
  let local: jasmine.SpyObj<PinpadLocalService>;
  let service: TrxPaymentService;
  let payment: TrxPaymentEntity;
  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('api', ['ExecutePostService']);
    local = jasmine.createSpyObj<PinpadLocalService>('local', ['process', 'status', 'acknowledge']);
    service = new TrxPaymentService(api, local);
    payment = Object.assign(new TrxPaymentEntity(), { PaymentPlatform: 'POS', PinpadPaymentId: 'reference-123' });
    api.ExecutePostService.and.resolveTo(Object.assign(new ResponseWsDto(), { Data: {} }));
  });
  it('libera el formulario si falla el login sin enviar ningun cobro', async () => {
    local.process.and.rejectWith(new Error('agent unavailable'));
    const response = await service.Save(payment);
    expect(response.ErrorStatus).toBeTrue();
    expect(response.Data.CanStartNewPayment).toBeTrue();
    expect(response.Data.PaymentStatus).toBe('NOT_SENT');
    expect(api.ExecutePostService.calls.count()).toBe(1);
  });
  it('libera si falla la preparacion y aun no hubo un intento de registro', async () => {
    api.ExecutePostService.and.resolveTo(Object.assign(new ResponseWsDto(), { ErrorStatus: true, Message: 'Invalid configuration' }));
    expect((await service.Save(payment)).Data.CanStartNewPayment).toBeTrue();
    expect(local.process).not.toHaveBeenCalled();
  });
  it('conserva el pendiente tras enviar, incluso si el siguiente login falla', async () => {
    local.process.and.callFake(async (_instructions, attempted) => {
      attempted!(); throw new Error('lost response');
    });
    expect((await service.Save(payment)).Data?.CanStartNewPayment).not.toBeTrue();
    local.process.and.rejectWith(new Error('agent now unavailable'));
    expect((await service.Save(payment)).Data?.CanStartNewPayment).not.toBeTrue();
  });
  it('conserva una aprobacion si falla el guardado central', async () => {
    payment.PinpadResult = { payload: 'approved', signature: 'signature' }; local.status.and.returnValue('APPROVED');
    api.ExecutePostService.and.rejectWith(new Error('lost central response'));
    expect((await service.Save(payment)).Data?.CanStartNewPayment).not.toBeTrue();
    expect(local.process).not.toHaveBeenCalled();
  });
  it('un pago directo no prepara ni consulta el pinpad', async () => {
    payment.PaymentPlatform = 'FISICO'; payment.PinpadPaymentId = null;
    expect((await service.Save(payment)).ErrorStatus).toBeFalse();
    expect(local.process).not.toHaveBeenCalled();
    expect(api.ExecutePostService.calls.count()).toBe(1);
  });
  it('POS_MANUAL no conecta, consulta ni confirma con el agente', async () => {
    payment.PaymentPlatform = 'POS_MANUAL'; payment.PinpadPaymentId = null;
    expect((await service.Save(payment)).ErrorStatus).toBeFalse();
    expect(local.process).not.toHaveBeenCalled();
    expect(local.status).not.toHaveBeenCalled();
    expect(local.acknowledge).not.toHaveBeenCalled();
    expect(api.ExecutePostService.calls.count()).toBe(1);
  });
});
