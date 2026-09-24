import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, NgZone, OnDestroy, Output, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { MercadoPagoBrickController, MercadoPagoCardData, MercadoPagoCheckoutDto, MercadoPagoRequestDto, MercadoPagoResultDto } from '../../model/dto/MercadoPagoDto';
import { MercadoPagoService } from '../../service/mercado-pago.service';

@Component({
  selector: 'app-mercado-pago-modal',
  templateUrl: './mercado-pago-modal.component.html',
  styleUrls: ['./mercado-pago-modal.component.css']
})
export class MercadoPagoModalComponent implements AfterViewInit, OnDestroy {
  @Input() public OrderToken = '';
  @Input() public PaymentMethodCod = '';
  @Output() public closed = new EventEmitter<void>();
  @Output() public paymentChanged = new EventEmitter<MercadoPagoResultDto | null>();
  @ViewChild('dialog', { static: true }) private dialog!: ElementRef<HTMLElement>;
  public Configuration: MercadoPagoCheckoutDto | null = null;
  public Payment: MercadoPagoResultDto | null = null;
  public IsLoading = true;
  public IsSubmitting = false;
  public ErrorMessage = '';
  public readonly ContainerId = 'mercado-pago-card-payment';
  public PendingRequest: MercadoPagoRequestDto | null = null;
  private brick: MercadoPagoBrickController | null = null;
  private destroyed = false;
  private previousFocus: HTMLElement | null = null;
  private previousOverflow = '';

  public constructor(private mercadoPagoService: MercadoPagoService, private ngZone: NgZone,
                     private toastrService: ToastrService) {}

  public ngAfterViewInit(): void {
    this.previousFocus = document.activeElement as HTMLElement;
    this.previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    this.dialog.nativeElement.focus();
    void this.initialize();
  }

  public ngOnDestroy(): void {
    this.destroyed = true;
    void this.unmount();
    document.body.style.overflow = this.previousOverflow;
    this.previousFocus?.focus();
  }

  @HostListener('document:keydown.escape')
  public close(): void { if (!this.IsSubmitting) this.closed.emit(); }

  @HostListener('keydown.tab', ['$event'])
  public keepFocus(event: KeyboardEvent): void {
    const elements = Array.from(this.dialog.nativeElement.querySelectorAll<HTMLElement>(
      'button:not([disabled]), input:not([disabled]), select:not([disabled]), iframe, [tabindex="0"]'
    )).filter(element => element.getClientRects().length > 0);
    const first = elements[0];
    const last = elements[elements.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === this.dialog.nativeElement)) {
      event.preventDefault(); last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault(); first?.focus();
    }
  }

  public async initialize(): Promise<void> {
    this.IsLoading = true;
    this.ErrorMessage = '';
    try {
      const response = await this.mercadoPagoService.configuration(this.OrderToken);
      if (this.destroyed) return;
      if (response.ErrorStatus) throw new Error(response.Message || 'No se pudo cargar la configuración de Mercado Pago.');
      this.Configuration = response.Data as MercadoPagoCheckoutDto;
      this.Payment = this.Configuration.Payment;
      if (this.Payment?.State === 'P' || this.Payment?.State === 'C') {
        this.paymentChanged.emit(this.Payment);
        this.IsLoading = false;
      } else {
        this.Payment = null;
        await this.renderBrick();
      }
    } catch (error) {
      this.showError(error);
    }
  }

  public async refresh(): Promise<void> {
    if (this.IsSubmitting) return;
    this.IsSubmitting = true;
    this.ErrorMessage = '';
    try {
      const response = await this.mercadoPagoService.status(this.OrderToken);
      if (this.destroyed) return;
      if (response.ErrorStatus) throw new Error(response.Message || 'No se pudo consultar el pago.');
      await this.applyResult(response.Data as MercadoPagoResultDto | null);
      if (!this.Payment) await this.renderBrick();
    } catch (error) { this.showError(error); }
    finally { this.IsSubmitting = false; }
  }

  public async retry(): Promise<void> {
    this.PendingRequest = null;
    this.Payment = null;
    this.ErrorMessage = '';
    await this.renderBrick();
  }

  public async retrySubmission(): Promise<void> {
    if (this.PendingRequest) await this.sendPayment(this.PendingRequest);
  }

  private async renderBrick(): Promise<void> {
    if (!this.Configuration || this.destroyed) return;
    this.IsLoading = true;
    try {
      await this.unmount();
      const MercadoPago = await this.mercadoPagoService.loadSdk();
      if (this.destroyed) return;
      const sdk = new MercadoPago(this.Configuration.PublicKey, { locale: this.Configuration.Locale });
      const controller = await sdk.bricks().create('cardPayment', this.ContainerId, {
        initialization: { amount: Number(this.Configuration.Amount) },
        customization: {
          paymentMethods: {
            maxInstallments: this.PaymentMethodCod === 'TD001' ? 1 : this.Configuration.MaxInstallments,
            types: { included: [this.PaymentMethodCod === 'TD001' ? 'debit_card' : 'credit_card'] }
          },
          visual: { style: { theme: 'bootstrap' } }
        },
        callbacks: {
          onReady: () => this.ngZone.run(() => { this.IsLoading = false; }),
          onError: error => this.ngZone.run(() => {
            this.IsLoading = false;
            if (error.type === 'critical') this.ErrorMessage = 'No se pudo iniciar el formulario de Mercado Pago. Cierra y vuelve a abrir el modal.';
          }),
          onSubmit: data => this.ngZone.run(() => this.submit(data))
        }
      });
      if (this.destroyed) await this.unmount(controller); else this.brick = controller;
    } catch (error) { this.showError(error); }
  }

  private async submit(data: MercadoPagoCardData): Promise<void> {
    if (this.IsSubmitting || this.Payment?.State === 'P' || this.Payment?.State === 'C') return;
    // Keep the same token and idempotency key on a retry; never persist the token in browser storage.
    this.PendingRequest = {
      OrderToken: this.OrderToken, PaymentMethodCod: this.PaymentMethodCod,
      AttemptId: crypto.randomUUID(),
      FormData: { token: data.token, payment_method_id: data.payment_method_id,
        issuer_id: data.issuer_id, installments: data.installments, payer: data.payer }
    };
    await this.sendPayment(this.PendingRequest);
  }

  private async sendPayment(request: MercadoPagoRequestDto): Promise<void> {
    if (this.IsSubmitting) return;
    this.IsSubmitting = true;
    this.ErrorMessage = '';
    try {
      const response = await this.mercadoPagoService.pay(request);
      if (this.destroyed) return;
      if (response.ErrorStatus) {
        await this.applyResult({ AttemptId: request.AttemptId, State: 'P',
          Message: 'No se pudo confirmar el resultado. Consulta el estado antes de realizar otro pago.' });
        throw new Error(response.Message || 'No se pudo confirmar la respuesta de Mercado Pago.');
      }
      await this.applyResult(response.Data as MercadoPagoResultDto);
    } catch (error) { this.showError(error); }
    finally { this.IsSubmitting = false; }
  }

  private async applyResult(payment: MercadoPagoResultDto | null): Promise<void> {
    this.Payment = payment;
    this.paymentChanged.emit(payment);
    if (payment) await this.unmount();
    if (payment?.State !== 'P') this.PendingRequest = null;
    if (payment?.State === 'C') this.toastrService.success(payment.Message);
  }

  private async unmount(controller = this.brick): Promise<void> {
    if (controller === this.brick) this.brick = null;
    try {
      // The SDK may return void. Cleanup must never replace the confirmed payment result.
      if (controller) await controller.unmount();
    } catch {
      // Teardown can fail if the SDK already removed its iframe during navigation.
    }
  }

  private showError(error: unknown): void {
    if (this.destroyed) return;
    this.IsLoading = false;
    this.ErrorMessage = error instanceof Error ? error.message : 'No se pudo procesar el pago.';
    this.toastrService.error(this.ErrorMessage);
  }
}
