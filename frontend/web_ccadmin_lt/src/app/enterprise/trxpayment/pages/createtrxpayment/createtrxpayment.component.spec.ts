import { ElementRef } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { CreditNoteService } from '../../../sale/service/CreditNote.service';
import { AlertService } from '../../../shared/service/AlertService';
import { PaymentMethodEntity } from '../../../shared/model/entity/PaymentMethodEntity';
import { CurrencyEntity } from '../../../shared/model/entity/CurrencyEntity';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { TrxPaymentService } from '../../service/TrxPaymentService';
import { CreatetrxpaymentComponent } from './createtrxpayment.component';

describe('CreatetrxpaymentComponent medios de pago', () => {
  let component: CreatetrxpaymentComponent;
  let payments: jasmine.SpyObj<TrxPaymentService>;
  let toastr: jasmine.SpyObj<ToastrService>;

  beforeEach(() => {
    payments = jasmine.createSpyObj<TrxPaymentService>('payments', ['Save']);
    toastr = jasmine.createSpyObj<ToastrService>('toastr', ['error', 'success']);
    const alerts = jasmine.createSpyObj<AlertService>('alerts', ['waringHtml']);
    component = new CreatetrxpaymentComponent(toastr, payments, {} as CreditNoteService, alerts);
    component.txtAmountPaid = new ElementRef(document.createElement('input'));
    component.txtAmountPaid.nativeElement.value = '50.00';
    component.cboPaymentMethodCod = new ElementRef(document.createElement('input'));
    component.cboCurrencyCod = new ElementRef(document.createElement('select'));
    component.cboCurrencyCod.nativeElement.add(new Option('PEN', 'PEN'));
    component.currencyList = [Object.assign(new CurrencyEntity(), { CurrencyCod: 'PEN', NumExchangevalue: 1 })];
    component.TrxPaymentComponenRequest.InputOutstandingBalance = 100;
    spyOn(component, 'confirmPayment').and.resolveTo({ isConfirmed: true });
    payments.Save.and.callFake(async payment => Object.assign(new ResponseWsDto(), { Data: payment, ErrorStatus: false }));
  });

  function select(code: string, type: string): void {
    component.paymentMethodList = [Object.assign(new PaymentMethodEntity(), {
      PaymentMethodCod: code, PaymentMethodType: type
    })];
    component.selectedPaymentMethodCod = code;
  }

  for (const [code, type] of [['TJ001', '1002'], ['TD001', '1002'], ['TC001', '1003'], ['WD001', '1006']]) {
    it(`inicia cobro POS para ${code} segun tipo ${type}`, async () => {
      select(code, type);
      await component.Save();
      const saved = payments.Save.calls.mostRecent().args[0];
      expect(saved.PaymentPlatform).toBe('POS');
      expect(saved.PaymentMethodCod).toBe(code);
      expect(saved.PaymentStatus).toBe('PENDING');
      expect(saved.PinpadPaymentId).toBeTruthy();
      expect(saved.AmountReturned).toBe(0);
    });
  }

  for (const [code, type] of [['E0001', '1001'], ['TB001', '1005'], ['OTHER', '9999']]) {
    it(`registra ${code} manualmente sin reutilizar el cobro POS anterior`, async () => {
      component.trxPayment = component.transactionPos();
      component.trxPayment.PinpadResult = { payload: 'old-proof', signature: 'old-signature' };
      select(code, type);
      await component.Save();
      const saved = payments.Save.calls.mostRecent().args[0];
      expect(saved.PaymentPlatform).toBe('FISICO');
      expect(saved.PaymentStatus).toBe('OK');
      expect(saved.PinpadPaymentId).toBeNull();
      expect(saved.PinpadResult).toBeUndefined();
      expect(component.pendingPinpadPayment).toBeNull();
    });
  }

  it('no permite sobrepago ni vuelto en Yape', async () => {
    select('WD001', '1006');
    component.txtAmountPaid.nativeElement.value = '101.00';
    await component.Save();
    expect(payments.Save).not.toHaveBeenCalled();
    expect(toastr.error).toHaveBeenCalled();
  });
});
