import { Component, Input } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { SalePaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/SalePaymentEntity';
import { TrxPaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/TrxPaymentEntity';
import { TrxPaymentDocumentEntity } from 'src/app/enterprise/trxpayment/model/entity/TrxPaymentDocumentEntity';
import { TrxPaymentService } from 'src/app/enterprise/trxpayment/service/TrxPaymentService';

@Component({
  selector: 'app-salepaymentdetail',
  templateUrl: './salepaymentdetail.component.html',
  styleUrls: ['./salepaymentdetail.component.css']
})
export class SalepaymentdetailComponent {

  @Input() paymentList: SalePaymentEntity[] = [];
  @Input() paymentMethodList: PaymentMethodEntity[] = [];
  @Input() currencyCod: string = '';

  documentList: TrxPaymentDocumentEntity[] = [];
  selectedPayment: SalePaymentEntity | null = null;
  isDocumentGalleryVisible: boolean = false;
  isLoadingDocuments: boolean = false;

  constructor(
    private trxPaymentService: TrxPaymentService,
    private toastrService: ToastrService
  ) {
  }

  async openDocumentGallery(payment: SalePaymentEntity): Promise<void> {
    const trxPaymentId = Number(payment?.TrxPayment?.TrxPaymentId || payment?.TrxPaymentId || 0);
    if (trxPaymentId <= 0) {
      this.toastrService.warning('El pago no tiene un identificador válido.');
      return;
    }

    this.selectedPayment = payment;
    this.documentList = [];
    this.isDocumentGalleryVisible = true;
    this.isLoadingDocuments = true;
    try {
      const response = await this.trxPaymentService.FindDocumentsByTrxPaymentId(trxPaymentId);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron consultar los archivos del pago.');
        return;
      }
      this.documentList = ((response.Data || []) as TrxPaymentDocumentEntity[]).map(document => {
        const result = Object.assign(new TrxPaymentDocumentEntity(), document);
        if (this.isImage(result) && result.ContentEncoding === 'BASE64') {
          result.PreviewUrl = `data:${result.ContentType};base64,${result.Content}`;
        }
        return result;
      });
    } finally {
      this.isLoadingDocuments = false;
    }
  }

  closeDocumentGallery(): void {
    this.isDocumentGalleryVisible = false;
    this.documentList = [];
    this.selectedPayment = null;
  }

  isImage(document: TrxPaymentDocumentEntity): boolean {
    return (document.ContentType || '').toLowerCase().startsWith('image/');
  }

  isText(document: TrxPaymentDocumentEntity): boolean {
    return document.ContentEncoding === 'TEXT' || document.ContentEncoding === 'JSON';
  }

  getDocumentTypeName(document: TrxPaymentDocumentEntity): string {
    const names: { [key: string]: string } = {
      PAYMENT_PROOF: 'Comprobante de pago',
      PINPAD_RECEIPT: 'Voucher de pinpad',
      PINPAD_RESPONSE: 'Respuesta de pinpad',
      OTHER: 'Otro documento'
    };
    return names[document.DocumentType] || document.DocumentType || 'Documento';
  }

  downloadDocument(document: TrxPaymentDocumentEntity): void {
    try {
      const content = document.ContentEncoding === 'BASE64'
        ? this.decodeBase64(document.Content)
        : new TextEncoder().encode(document.Content || '');
      const blob = new Blob([content], { type: document.ContentType || 'application/octet-stream' });
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = document.FileName || `pago-${document.TrxPaymentId}-${document.TrxPaymentDocumentId}`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (_error) {
      this.toastrService.error('No se pudo descargar el archivo.');
    }
  }

  private decodeBase64(content: string): Uint8Array {
    const binary = atob(content || '');
    const result = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index++) {
      result[index] = binary.charCodeAt(index);
    }
    return result;
  }

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
