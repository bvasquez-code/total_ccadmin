import { Component, Input } from '@angular/core';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { SalePaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/SalePaymentEntity';
import { TrxPaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/TrxPaymentEntity';

@Component({
  selector: 'app-salepaymentdetail',
  templateUrl: './salepaymentdetail.component.html'
})
export class SalepaymentdetailComponent {

  @Input() paymentList: SalePaymentEntity[] = [];
  @Input() paymentMethodList: PaymentMethodEntity[] = [];
  @Input() currencyCod: string = '';

  getPaymentMethodName(payment: TrxPaymentEntity): string {
    if (this.isCreditNoteApplication(payment)) return 'Saldo de nota de crédito';

    const paymentMethod = this.paymentMethodList.find(
      item => item.PaymentMethodCod === payment.PaymentMethodCod
    );
    return paymentMethod?.Description || paymentMethod?.Name || payment.PaymentMethodCod || '-';
  }

  getPaymentAmount(payment: SalePaymentEntity): number {
    return Number(
      payment.TrxPayment?.AmountPaid
      ?? payment.NumAmountPaidOrigin
      ?? payment.NumAmountPaid
      ?? 0
    );
  }

  getPaymentCurrency(payment: SalePaymentEntity): string {
    return payment.TrxPayment?.CurrencyCod || payment.CurrencyCod || this.currencyCod;
  }

  getReturnedAmount(payment: SalePaymentEntity): number {
    return Number(payment.TrxPayment?.AmountReturned ?? payment.NumAmountReturned ?? 0);
  }

  getPaymentDate(payment: SalePaymentEntity): Date {
    return payment.TrxPayment?.CreationDate || payment.CreationDate;
  }

  getPaymentStatus(payment: SalePaymentEntity): string {
    if (payment.Status === 'I' || payment.TrxPayment?.Status === 'I') return 'Inactivo';
    if (payment.TrxPayment?.TypeMovement === 'E') return 'Reversión';
    if (this.isCreditNoteApplication(payment.TrxPayment)) return 'Crédito aplicado';
    return payment.TrxPayment?.PaymentStatus || 'Registrado';
  }

  getNetPaid(): number {
    return this.toMoney((this.paymentList || [])
      .filter(item => item.Status !== 'I' && item.TrxPayment?.Status !== 'I')
      .reduce((sum, item) => sum + Number(item.NumAmountPaid || 0), 0));
  }

  isCreditNoteApplication(payment: TrxPaymentEntity | null | undefined): boolean {
    return payment?.PaymentMethodCod === 'NC001'
      && payment?.PaymentPlatform === 'CREDITO_INTERNO';
  }

  private toMoney(value: number): number {
    return Math.round(Number(value || 0) * 100) / 100;
  }
}
