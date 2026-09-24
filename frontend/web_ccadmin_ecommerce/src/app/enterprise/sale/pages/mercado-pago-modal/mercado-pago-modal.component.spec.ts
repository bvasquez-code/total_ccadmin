import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastrService } from 'ngx-toastr';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { MercadoPagoConstructor } from '../../model/dto/MercadoPagoDto';
import { MercadoPagoService } from '../../service/mercado-pago.service';
import { MercadoPagoModalComponent } from './mercado-pago-modal.component';

describe('MercadoPagoModalComponent', () => {
  let fixture: ComponentFixture<MercadoPagoModalComponent>;
  let component: MercadoPagoModalComponent;
  let service: jasmine.SpyObj<MercadoPagoService>;
  let settings: any;
  let create: jasmine.Spy;
  let unmount: jasmine.Spy;
  const ok = (Data: unknown): ResponseWsDto => Object.assign(new ResponseWsDto(), { ErrorStatus: false, Data });

  beforeEach(async () => {
    service = jasmine.createSpyObj('MercadoPagoService', ['configuration', 'loadSdk', 'pay', 'status']);
    unmount = jasmine.createSpy('unmount').and.returnValue(undefined);
    create = jasmine.createSpy('create').and.callFake((_type: string, _container: string, supplied: any) => {
      settings = supplied;
      supplied.callbacks.onReady();
      return Promise.resolve({ unmount });
    });
    service.loadSdk.and.resolveTo(class {
      bricks(): unknown { return { create }; }
    } as unknown as MercadoPagoConstructor);
    service.configuration.and.resolveTo(ok({
      PublicKey: 'test-public-key', Locale: 'es-PE', CurrencyCod: 'PEN', Amount: 50.5,
      MaxInstallments: 6, TestMode: true, Payment: null
    }));
    await TestBed.configureTestingModule({
      imports: [CommonModule], declarations: [MercadoPagoModalComponent],
      providers: [
        { provide: MercadoPagoService, useValue: service },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['error', 'success']) }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(MercadoPagoModalComponent);
    component = fixture.componentInstance;
    component.OrderToken = 'order-token';
    component.PaymentMethodCod = 'TD001';
  });

  async function initialize(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => fixture.destroy());

  it('renders only debit cards with one installment and the server amount', async () => {
    await initialize();
    expect(settings.initialization.amount).toBe(50.5);
    expect(settings.customization.paymentMethods.types.included).toEqual(['debit_card']);
    expect(settings.customization.paymentMethods.maxInstallments).toBe(1);
    expect(service.pay).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Modo de prueba');
  });

  it('keeps the original token and idempotency key after a lost response', async () => {
    await initialize();
    service.pay.and.resolveTo(Object.assign(new ResponseWsDto(), { ErrorStatus: true, Message: 'Conexión interrumpida' }));
    await settings.callbacks.onSubmit({ token: 'token1', installments: 1, payment_method_id: 'debvisa',
      transaction_amount: 0.01, payer: { email: 'buyer@example.com' } });
    const first = service.pay.calls.mostRecent().args[0];
    expect(component.Payment?.State).toBe('P');
    expect((first.FormData as any).transaction_amount).toBeUndefined();
    await component.retrySubmission();
    expect(service.pay.calls.mostRecent().args[0]).toEqual(first);
    expect(unmount).toHaveBeenCalled();
  });

  it('reopens a pending order without rendering a second payment form', async () => {
    service.configuration.and.resolveTo(ok({ PublicKey: 'test', Amount: 50.5,
      Payment: { AttemptId: 'attempt1', State: 'P', Message: 'Pendiente' } }));
    await initialize();
    expect(create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Consultar estado');
    service.status.and.resolveTo(ok({ AttemptId: 'attempt1', State: 'C', Message: 'Aprobado' }));
    const changed = spyOn(component.paymentChanged, 'emit');
    await component.refresh();
    expect(component.Payment?.State).toBe('C');
    expect(changed).toHaveBeenCalledWith(jasmine.objectContaining({ State: 'C' }));
    expect(service.pay).not.toHaveBeenCalled();
  });

  it('shows missing configuration without loading the external SDK', async () => {
    service.configuration.and.resolveTo(Object.assign(new ResponseWsDto(), { ErrorStatus: true, Message: 'No disponible' }));
    await initialize();
    expect(service.loadSdk).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('No disponible');
  });

  it('unmounts the SDK when the dialog is destroyed', async () => {
    await initialize();
    fixture.destroy();
    expect(unmount).toHaveBeenCalledTimes(1);
  });

  for (const state of ['C', 'F'] as const) {
    for (const cleanup of ['void', 'promise', 'rejection', 'throw'] as const) {
      it(`preserves payment ${state} when SDK unmount returns ${cleanup}`, async () => {
        if (cleanup === 'promise') unmount.and.resolveTo();
        if (cleanup === 'rejection') unmount.and.callFake(() => Promise.reject(new Error('SDK cleanup failed')));
        if (cleanup === 'throw') unmount.and.throwError('SDK cleanup failed');
        await initialize();
        service.pay.and.resolveTo(ok({ AttemptId: 'attempt1', PaymentId: '1352512313', State: state,
          Message: state === 'C' ? 'Pago aprobado' : 'Pago rechazado' }));
        await settings.callbacks.onSubmit({ token: 'token1', installments: 1, payment_method_id: 'debvisa',
          payer: { email: 'buyer@example.com' } });
        expect(component.Payment?.State).toBe(state);
        expect(component.ErrorMessage).toBe('');
        expect(component.PendingRequest).toBeNull();
        expect(component.IsSubmitting).toBeFalse();
        expect(unmount).toHaveBeenCalledTimes(1);
      });
    }
  }
});
