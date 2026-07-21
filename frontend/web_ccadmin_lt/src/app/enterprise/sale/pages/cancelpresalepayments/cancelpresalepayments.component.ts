import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
import { TrxPaymentComponenRequestDto } from 'src/app/enterprise/trxpayment/model/dto/TrxPaymentComponenRequestDto';
import { SalePaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/SalePaymentEntity';
import { TrxPaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/TrxPaymentEntity';
import { PresaleCancellationDetailDto } from '../../model/dto/PresaleCancellationDetailDto';
import { SalePaymentRegisterDto } from '../../model/dto/SalePaymentRegisterDto';
import { PresaleService } from '../../service/presale.service';
import { SaleService } from '../../service/sale.service';

@Component({
  selector: 'app-cancelpresalepayments',
  templateUrl: './cancelpresalepayments.component.html'
})
export class CancelpresalepaymentsComponent implements OnInit {

  presaleCod: string = '';
  cancellationMode: 'regular' | 'forced' = 'regular';
  cancellationDetail: PresaleCancellationDetailDto = new PresaleCancellationDetailDto();
  trxPaymentRequest: TrxPaymentComponenRequestDto = new TrxPaymentComponenRequestDto();
  isLoading: boolean = true;

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private toastrService: ToastrService,
    private alertService: AlertService,
    private presaleService: PresaleService,
    private saleService: SaleService
  ) {}

  ngOnInit(): void {
    this.presaleCod = this.activatedRoute.snapshot.queryParamMap.get('PresaleCod') || '';
    this.cancellationMode = this.activatedRoute.snapshot.queryParamMap.get('Mode') === 'forced'
      ? 'forced'
      : 'regular';

    if (!this.presaleCod) {
      this.toastrService.error('Debe indicar la preventa que desea anular.');
      this.router.navigate(['/enterprise/sale/pages/listpresale']);
      return;
    }
    this.loadCancellationDetail();
  }

  async loadCancellationDetail(): Promise<void> {
    this.isLoading = true;
    const response: ResponseWsDto = await this.presaleService.cancellationDetail(this.presaleCod);
    this.isLoading = false;

    if (response.ErrorStatus) {
      this.toastrService.error(response.Message);
      return;
    }

    this.cancellationDetail = response.Data;
    this.prepareReversalRequest();
  }

  prepareReversalRequest(): void {
    const paymentList: SalePaymentEntity[] = this.cancellationDetail.SaleDetail?.DetailPayment ?? [];
    const originalPaymentList: TrxPaymentEntity[] = paymentList
      .filter(item => item.TrxPayment && item.TrxPayment.TypeMovement !== 'E')
      .map(item => this.buildOriginalPayment(item));
    const reversalPaymentList: TrxPaymentEntity[] = paymentList
      .map(item => item.TrxPayment)
      .filter(item => item && item.TypeMovement === 'E');

    const request: TrxPaymentComponenRequestDto = new TrxPaymentComponenRequestDto();
    request.InputTypeMovement = 'E';
    request.InputOutstandingBalance = 0;
    request.InputReversalAmount = this.toMoney(
      originalPaymentList.reduce((sum, item) => sum + Math.abs(Number(item.AmountPaid || 0)), 0)
    );
    request.TrxPaymentReversalList = originalPaymentList;
    request.TrxPaymentList = reversalPaymentList;
    this.trxPaymentRequest = request;
  }

  buildOriginalPayment(salePayment: SalePaymentEntity): TrxPaymentEntity {
    const payment: TrxPaymentEntity = Object.assign(new TrxPaymentEntity(), salePayment.TrxPayment);
    const exchangeValue: number = Number(salePayment.NumExchangevalue || 1);
    payment.AmountPaid = this.toMoney(Math.abs(Number(salePayment.NumAmountPaid || 0)) / exchangeValue);
    payment.AmountReturned = 0;
    return payment;
  }

  async registerReversal(payment: TrxPaymentEntity): Promise<void> {
    if (!payment) return;

    const request: SalePaymentRegisterDto = new SalePaymentRegisterDto();
    request.SaleCod = this.cancellationDetail.SaleDetail?.Headboard?.SaleCod ?? '';
    request.TrxPaymentId = payment.TrxPaymentId;
    const response: ResponseWsDto = await this.saleService.AddReversalPayment(request);

    if (response.ErrorStatus) {
      this.trxPaymentRequest.TrxPaymentList = this.trxPaymentRequest.TrxPaymentList
        .filter(item => item.TrxPaymentId !== payment.TrxPaymentId);
      this.toastrService.error(response.Message);
      return;
    }

    await this.loadCancellationDetail();
    if (this.hasPendingPayments()) {
      this.toastrService.success('Reversa registrada. Continue con el siguiente pago.');
    } else {
      this.toastrService.success('Todos los pagos fueron anulados. Ya puede finalizar la anulacion.');
    }
  }

  async finishCancellation(): Promise<void> {
    if (this.hasPendingPayments()) {
      this.toastrService.error('Debe anular todos los pagos antes de finalizar.');
      return;
    }

    const modeLabel: string = this.cancellationMode === 'forced' ? 'forzada' : 'regular';
    const confirmation = await this.alertService.waring(
      `Se cambiara el estado de la preventa y de su venta mediante anulacion ${modeLabel}.`,
      'Finalizar anulacion de preventa'
    );
    if (!confirmation.isConfirmed) return;

    const response: ResponseWsDto = this.cancellationMode === 'forced'
      ? await this.presaleService.forceCancel(this.presaleCod)
      : await this.presaleService.cancel(this.presaleCod);

    if (response.ErrorStatus) {
      this.toastrService.error(response.Message);
      return;
    }

    this.toastrService.success('Preventa anulada correctamente.');
    await this.router.navigate(['/enterprise/sale/pages/listpresale']);
  }

  hasPendingPayments(): boolean {
    return Number(this.cancellationDetail.PendingPaymentAmount || 0) > 0.009;
  }

  toMoney(value: number): number {
    return Math.round(Number(value || 0) * 100) / 100;
  }
}
