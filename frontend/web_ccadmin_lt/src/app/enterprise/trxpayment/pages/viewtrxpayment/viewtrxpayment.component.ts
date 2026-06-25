import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyEntity } from 'src/app/enterprise/shared/model/entity/CurrencyEntity';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { TrxPaymentEntity } from '../../model/entity/TrxPaymentEntity';
import { TrxPaymentService } from '../../service/TrxPaymentService';

@Component({
  selector: 'app-viewtrxpayment',
  templateUrl: './viewtrxpayment.component.html'
})
export class ViewtrxpaymentComponent {

  TrxPaymentId: number = 0;
  TrxPayment: TrxPaymentEntity = new TrxPaymentEntity();
  paymentMethodList: PaymentMethodEntity[] = [];
  currencyList: CurrencyEntity[] = [];

  constructor(
    private trxPaymentService: TrxPaymentService,
    private router: Router
  ) {
    const urlTree: any = this.router.parseUrl(this.router.url);
    this.TrxPaymentId = Number(urlTree.queryParams['TrxPaymentId'] ?? 0);
    this.FindDataFormView(this.TrxPaymentId);
  }

  async FindDataFormView(TrxPaymentId: number): Promise<void> {
    if (!TrxPaymentId) return;

    const rpt = await this.trxPaymentService.FindDataFormView(TrxPaymentId);
    if (!rpt.ErrorStatus) {
      this.TrxPayment = rpt.DataAdditional.find(e => e.Name === 'trxPayment')?.Data ?? new TrxPaymentEntity();
      this.paymentMethodList = rpt.DataAdditional.find(e => e.Name === 'paymentMethodList')?.Data ?? [];
      this.currencyList = rpt.DataAdditional.find(e => e.Name === 'currencyList')?.Data ?? [];
    }
  }

  getStatusLabel(status: string): string {
    const map: { [key: string]: string } = {
      OK: 'OK',
      P: 'Pendiente',
      F: 'Fallido'
    };
    return map[status] ?? status;
  }

  getStatusClass(status: string): string {
    const map: { [key: string]: string } = {
      OK: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
      P: 'badge badge-sm bgc-orange-d1 text-white pb-1 px-25',
      F: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
    };
    return map[status] ?? 'badge badge-sm bgc-secondary-l2 text-dark pb-1 px-25';
  }

  getMovementLabel(typeMovement: string): string {
    const map: { [key: string]: string } = {
      I: 'Ingreso',
      E: 'Egreso'
    };
    return map[typeMovement] ?? typeMovement;
  }

  getMovementClass(typeMovement: string): string {
    const map: { [key: string]: string } = {
      I: 'badge badge-sm bgc-green-d1 text-white pb-1 px-25',
      E: 'badge badge-sm bgc-orange-d1 text-white pb-1 px-25'
    };
    return map[typeMovement] ?? 'badge badge-sm bgc-secondary-l2 text-dark pb-1 px-25';
  }

  getPaymentMethodName(PaymentMethodCod: string): string {
    const paymentMethod = this.paymentMethodList.find(e => e.PaymentMethodCod === PaymentMethodCod);
    return paymentMethod?.Name || paymentMethod?.Description || PaymentMethodCod || '-';
  }

  getCurrencyName(CurrencyCod: string): string {
    const currency = this.currencyList.find(e => e.CurrencyCod === CurrencyCod);
    return currency ? `${currency.CurrencyCod} - ${currency.CurrencyName}` : (CurrencyCod || '-');
  }

  goBack(): void {
    this.router.navigate(['/enterprise/trxpayment/pages/listtrxpayment']);
  }
}
