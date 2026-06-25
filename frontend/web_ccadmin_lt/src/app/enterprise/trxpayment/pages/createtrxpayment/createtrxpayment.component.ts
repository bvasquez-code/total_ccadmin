import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { TrxPaymentEntity } from '../../model/entity/TrxPaymentEntity';
import { IRegisterForm } from 'src/app/enterprise/shared/interface/IRegisterForm';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { TrxPaymentService } from '../../service/TrxPaymentService';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CurrencyEntity } from 'src/app/enterprise/shared/model/entity/CurrencyEntity';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { CreditNoteService } from 'src/app/enterprise/sale/service/CreditNote.service';
import { CreditNoteDetailDto } from 'src/app/enterprise/sale/model/dto/CreditNoteDetailDto';
import { TrxPaymentComponenRequestDto } from '../../model/dto/TrxPaymentComponenRequestDto';
import { ElementHtmlDto } from 'src/app/enterprise/shared/model/dto/ElementHtmlDto';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';

@Component({
  selector: 'app-createtrxpayment',
  templateUrl: './createtrxpayment.component.html'
})
export class CreatetrxpaymentComponent implements OnInit,IRegisterForm<TrxPaymentEntity,number>{

  @Input() TrxPaymentComponenRequest : TrxPaymentComponenRequestDto = new TrxPaymentComponenRequestDto(); 
  @Output() ResultForm = new EventEmitter<TrxPaymentEntity>();

  @ViewChild('cboPaymentMethodCod') cboPaymentMethodCod!: ElementRef<HTMLInputElement>;
  @ViewChild('cboCurrencyCod') cboCurrencyCod!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtAmountPaid') txtAmountPaid!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDocumentCod') txtDocumentCod!: ElementRef<HTMLInputElement>;
  

  paymentMethodList: PaymentMethodEntity[] = [];
  currencyList : CurrencyEntity[] = [];
  trxPayment : TrxPaymentEntity = new TrxPaymentEntity();
  txtDocumentVisible : boolean = false;
  creditNoteDetail : CreditNoteDetailDto = new CreditNoteDetailDto();
  selectedPaymentMethodCod: string = "";
  showPaymentMethodDropdown: boolean = false;

  txtAmountPaidConfigHtml : ElementHtmlDto = new ElementHtmlDto();
  cboCurrencyCodConfigHtml : ElementHtmlDto = new ElementHtmlDto();

  constructor(
    private toastrService : ToastrService,
    private trxPaymentService : TrxPaymentService,
    private creditNoteService : CreditNoteService,
    private alertService : AlertService
  ){
    setTimeout(() => {this.loadingModal();}, 100);
  }

  ngOnInit(): void {
    this.FindDataForm(0);
  }

  GetParamUrl(router: Router): void {
    throw new Error('Method not implemented.');
  }

  async FindDataForm(Id: number): Promise<void> {
    
    const rpt : ResponseWsDto = await this.trxPaymentService.FindDataForm();

    if(!rpt.ErrorStatus){
      this.paymentMethodList = rpt.DataAdditional.find( e => e.Name === "paymentMethodList" )?.Data ?? [];
      this.currencyList = rpt.DataAdditional.find( e => e.Name === "currencyList" )?.Data ?? [];
      this.selectedPaymentMethodCod = this.paymentMethodList[0]?.PaymentMethodCod ?? "";
    }
  }

  LoadingForm(Entity: TrxPaymentEntity): void {
    throw new Error('Method not implemented.');
  }

  async Save(): Promise<void> {

    if (this.isReversalMode()) {
      await this.SaveReversal();
      return;
    }

    let PaymentMethodCodSelect : string = this.selectedPaymentMethodCod || this.cboPaymentMethodCod.nativeElement.value;
    let CurrencyCodSelect : string = this.cboCurrencyCod.nativeElement.value;

    let paymentMethod : undefined | PaymentMethodEntity = this.paymentMethodList.find( e => e.PaymentMethodCod ===  PaymentMethodCodSelect );
    let Currency : undefined | CurrencyEntity = this.currencyList.find( e => e.CurrencyCod ===  CurrencyCodSelect );

    let outstandingBalance : number = Number(this.TrxPaymentComponenRequest.InputOutstandingBalance);

    if(paymentMethod){

      if(this.IsCash(paymentMethod)){
        this.trxPayment = this.transactionCash();
      }
      if(this.IsCard(paymentMethod)){
        this.trxPayment = this.transactionPos();
      }
      if(this.IsMethodPaymentOwn(paymentMethod)){
        this.trxPayment = this.transactionCreditNote();
      }
      this.trxPayment.PaymentMethodCod = paymentMethod.PaymentMethodCod;
    }

    if(Currency){
      this.trxPayment.CurrencyCod = Currency.CurrencyCod;
      this.trxPayment.NumExchangevalue = Currency.NumExchangevalue;
    }

    this.trxPayment.AmountPaid = Number(this.txtAmountPaid.nativeElement.value);
    this.trxPayment.TypeMovement = 'I'; // Ingreso

    if (this.trxPayment.AmountPaid <= 0) {
      this.toastrService.error("El monto a pagar debe ser mayor a cero.");
      return;
    }

    if (outstandingBalance <= 0) {
      this.toastrService.error("Ya no existe saldo por pagar.");
      return;
    }
    if(paymentMethod){
      if(this.IsCard(paymentMethod) && this.trxPayment.AmountPaid > outstandingBalance){
        this.toastrService.error("Para este tipos de medios de pago no se puede pagar montos superiores al saldo.");
        return;
      }

      if(this.IsCash(paymentMethod) && this.trxPayment.AmountPaid > outstandingBalance){
        this.trxPayment.AmountReturned = this.trxPayment.AmountPaid - outstandingBalance;
      }

    }


    const confirmResult = await this.confirmPayment(this.trxPayment, paymentMethod);

    if (!confirmResult?.isConfirmed) return;

    const rpt : ResponseWsDto = await this.trxPaymentService.Save(this.trxPayment);

    if(!rpt.ErrorStatus){

      const trxPaymentResult : TrxPaymentEntity = rpt.Data;

      this.TrxPaymentComponenRequest.TrxPaymentList.push(trxPaymentResult);

      this.EmitResultForm(trxPaymentResult);

      this.txtAmountPaid.nativeElement.value = "";

      this.toastrService.success("Operación realizada con exito.");

      this.toastrService.success("Se realiza el pago exitosamente");
    }else{
      this.toastrService.error(rpt.Message);
    }

  }

  async SaveReversal(): Promise<void> {
    const amountToReverse: number = this.getCurrentReversalAmount();

    if (amountToReverse <= 0) {
      this.toastrService.error("El monto a revertir debe ser mayor a cero.");
      return;
    }

    const reversalLimit: number = this.getReversalLimit();

    if (reversalLimit <= 0) {
      this.toastrService.error("Ya no existe saldo por revertir.");
      return;
    }

    if (amountToReverse > reversalLimit) {
      this.toastrService.error("El monto a revertir no puede ser mayor al saldo disponible para reversion.");
      return;
    }

    const trxPayment: TrxPaymentEntity | null = this.buildNextReversalPayment();

    if (!trxPayment) {
      this.toastrService.error("No existen pagos disponibles para revertir.");
      return;
    }

    const confirmResult = await this.confirmReversalPayment(trxPayment);

    if (!confirmResult?.isConfirmed) return;

    const rpt: ResponseWsDto = await this.trxPaymentService.Save(trxPayment);

    if (rpt.ErrorStatus) {
      this.toastrService.error(rpt.Message);
      return;
    }

    const trxPaymentResult: TrxPaymentEntity = rpt.Data;

    this.TrxPaymentComponenRequest.TrxPaymentList.push(trxPaymentResult);

    this.EmitResultForm(trxPaymentResult);

    this.txtAmountPaid.nativeElement.value = String(this.getCurrentReversalAmount());

    this.toastrService.success("Se realiza la reversion exitosamente");
  }

  buildNextReversalPayment(): TrxPaymentEntity | null {
    const payment: TrxPaymentEntity | undefined = this.getPaymentsToReverse()[0];

    if (!payment) return null;

    const amountToReverse: number = this.getCurrentReversalAmount();

    if (amountToReverse <= 0) return null;

    return this.clonePaymentForReversal(payment, amountToReverse);
  }

  async confirmReversalPayment(trxPayment: TrxPaymentEntity): Promise<any> {
    const originalPayment: TrxPaymentEntity | undefined = this.getOriginalPayment(trxPayment.ReversalOfTrxPaymentId);
    const paymentMethodDescription: string = this.getPaymentDescription(trxPayment.PaymentMethodCod) || trxPayment.PaymentMethodCod;
    const amount: string = `${trxPayment.CurrencyCod} ${this.toMoney(Math.abs(Number(trxPayment.AmountPaid || 0))).toFixed(2)}`;
    const originalId: string = originalPayment ? String(originalPayment.TrxPaymentId) : "";
    const transactionId: string = originalPayment?.TransactionId ? originalPayment.TransactionId : "-";
    const cardMessage: string = this.isCardPayment(trxPayment)
      ? `<div class="mt-2 text-danger"><i class="fa fa-credit-card mr-1"></i> Pase la tarjeta por el pinpad y confirme solo si la operacion fue aceptada.</div>`
      : "";
    const paymentMethodMedia: string = this.getPaymentMethodAlertMedia(trxPayment);
    const message: string = `
      <div class="text-left">
        <div class="text-center mb-3">
          ${paymentMethodMedia}
        </div>
        <div>Se va a revertir el pago <b>${originalId}</b>.</div>
        <div class="mt-2">Monto a revertir: <b class="text-danger" style="font-size: 1.2rem;">${amount}</b></div>
        <div>Medio de pago: <b>${paymentMethodDescription}</b></div>
        <div>Plataforma: <b>${trxPayment.PaymentPlatform}</b></div>
        <div>Referencia: <b>${transactionId}</b></div>
        ${cardMessage}
      </div>
    `;

    return await this.alertService.waringHtml(message, "Confirmar reversion de pago");
  }

  async confirmPayment(trxPayment: TrxPaymentEntity, paymentMethod?: PaymentMethodEntity): Promise<any> {
    const paymentMethodDescription: string = paymentMethod?.Description || paymentMethod?.Name || trxPayment.PaymentMethodCod;
    const amount: string = `${trxPayment.CurrencyCod} ${this.toMoney(Number(trxPayment.AmountPaid || 0)).toFixed(2)}`;
    const returnedAmount: string = trxPayment.AmountReturned > 0
      ? `<div>Vuelto: <b>${trxPayment.CurrencyCod} ${this.toMoney(Number(trxPayment.AmountReturned || 0)).toFixed(2)}</b></div>`
      : "";
    const transactionId: string = trxPayment.TransactionId ? trxPayment.TransactionId : "-";
    const cardMessage: string = this.isCardPayment(trxPayment)
      ? `<div class="mt-2 text-danger"><i class="fa fa-credit-card mr-1"></i> Pase la tarjeta por el pinpad y confirme solo si la operacion fue aceptada.</div>`
      : "";
    const paymentMethodMedia: string = this.getPaymentMethodAlertMedia(trxPayment);
    const message: string = `
      <div class="text-left">
        <div class="text-center mb-3">
          ${paymentMethodMedia}
        </div>
        <div>Se va a registrar un pago.</div>
        <div class="mt-2">Monto a pagar: <b class="text-success" style="font-size: 1.2rem;">${amount}</b></div>
        <div>Medio de pago: <b>${paymentMethodDescription}</b></div>
        <div>Plataforma: <b>${trxPayment.PaymentPlatform}</b></div>
        <div>Referencia: <b>${transactionId}</b></div>
        ${returnedAmount}
        ${cardMessage}
      </div>
    `;

    return await this.alertService.waringHtml(message, "Confirmar pago");
  }

  clonePaymentForReversal(payment: TrxPaymentEntity, amountToReverse: number): TrxPaymentEntity {
    const trxPayment: TrxPaymentEntity = new TrxPaymentEntity();

    trxPayment.PaymentMethodCod = payment.PaymentMethodCod;
    trxPayment.PaymentPlatform = payment.PaymentPlatform;
    trxPayment.CardNumber = payment.CardNumber;
    trxPayment.CardHolderName = payment.CardHolderName;
    trxPayment.CardExpirationDate = payment.CardExpirationDate;
    trxPayment.CardCVV = payment.CardCVV;
    trxPayment.TransactionId = null;
    trxPayment.PaymentStatus = payment.PaymentStatus || "OK";
    trxPayment.CurrencyCod = payment.CurrencyCod;
    trxPayment.CurrencyCodSys = payment.CurrencyCodSys;
    trxPayment.NumExchangevalue = payment.NumExchangevalue;
    trxPayment.AmountPaid = -1 * this.toMoney(amountToReverse);
    trxPayment.AmountReturned = 0;
    trxPayment.TypeMovement = 'E';
    trxPayment.ReversalOfTrxPaymentId = payment.TrxPaymentId;

    return trxPayment;
  }

  transactionPos():TrxPaymentEntity{

    let TrxPayment : TrxPaymentEntity = new TrxPaymentEntity();

    TrxPayment.PaymentPlatform = "POS";
    TrxPayment.CardNumber = "4578************"
    TrxPayment.CardHolderName = "Cliente Generico"
    TrxPayment.PaymentStatus = "OK";
    TrxPayment.TransactionId = this.generateIdFromDate();

    return TrxPayment;

  }

  transactionCash():TrxPaymentEntity{

    let TrxPayment : TrxPaymentEntity = new TrxPaymentEntity();

    TrxPayment.PaymentPlatform = "FISICO";
    TrxPayment.PaymentStatus = "OK";

    return TrxPayment;

  }

  transactionCreditNote():TrxPaymentEntity{

    let TrxPayment : TrxPaymentEntity = new TrxPaymentEntity();

    TrxPayment.PaymentPlatform = "DOCUMENTO";
    TrxPayment.PaymentStatus = "OK";
    TrxPayment.TransactionId = this.creditNoteDetail.Document.CounterfoilCod+"-"+this.creditNoteDetail.Document.DocumentCod;

    return TrxPayment;

  }

  IsCash(paymentMethod : PaymentMethodEntity){
    return (paymentMethod.PaymentMethodType === "1001");
}

  IsCard(paymentMethod : PaymentMethodEntity){
      return (paymentMethod.PaymentMethodType === "1002" || paymentMethod.PaymentMethodType === "1003");
  }

  IsMethodPaymentOwn(paymentMethod : PaymentMethodEntity){
    return (paymentMethod.PaymentMethodType === "1015");
  }

  generateIdFromDate(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = (now.getMonth() + 1).toString().padStart(2, '0');
    const day = now.getDate().toString().padStart(2, '0');
    const hours = now.getHours().toString().padStart(2, '0');
    const minutes = now.getMinutes().toString().padStart(2, '0');
    const seconds = now.getSeconds().toString().padStart(2, '0');
    const milliseconds = now.getMilliseconds().toString().padStart(3, '0');

    const id = `${year}${month}${day}_${hours}${minutes}${seconds}_${milliseconds}`;
    return id;
  }

  EmitResultForm(trxPayment : TrxPaymentEntity)
  {
    this.ResultForm.emit(trxPayment);
  }

  loadingModal(){
    
  }

  selectPaymentMethodCod(){
    let PaymentMethodCodSelect : string = this.selectedPaymentMethodCod || this.cboPaymentMethodCod.nativeElement.value;

    this.txtDocumentVisible = (PaymentMethodCodSelect === 'NC001');
    this.txtAmountPaidConfigHtml.ReadOnly = (PaymentMethodCodSelect === 'NC001');
    this.cboCurrencyCodConfigHtml.ReadOnly = (PaymentMethodCodSelect === 'NC001');
    this.txtAmountPaid.nativeElement.value = (PaymentMethodCodSelect === 'NC001') ? "0" : String(this.TrxPaymentComponenRequest.InputOutstandingBalance);
  }

  selectPaymentMethod(paymentMethod: PaymentMethodEntity): void {
    this.selectedPaymentMethodCod = paymentMethod.PaymentMethodCod;
    this.showPaymentMethodDropdown = false;
    this.selectPaymentMethodCod();
  }

  isPaymentMethodSelected(paymentMethod: PaymentMethodEntity): boolean {
    return this.selectedPaymentMethodCod === paymentMethod.PaymentMethodCod;
  }

  togglePaymentMethodDropdown(): void {
    this.showPaymentMethodDropdown = !this.showPaymentMethodDropdown;
  }

  closePaymentMethodDropdown(): void {
    setTimeout(() => {
      this.showPaymentMethodDropdown = false;
    }, 200);
  }

  getSelectedPaymentMethod(): PaymentMethodEntity | undefined {
    return this.getPaymentMethod(this.selectedPaymentMethodCod);
  }

  isReversalMode(): boolean {
    return this.TrxPaymentComponenRequest.InputTypeMovement === 'E';
  }

  getInputAmountLabel(): string {
    return this.isReversalMode() ? "Monto total a devolver" : "Saldo por pagar";
  }

  getAmountLabel(): string {
    return this.isReversalMode() ? "Monto a devolver ahora" : "Monto";
  }

  getTitle(): string {
    return this.isReversalMode() ? "Registrar Reversion de Pago" : "Registrar Pago";
  }

  getSaveButtonLabel(): string {
    return this.isReversalMode() ? "Revertir siguiente pago" : "Guardar";
  }

  getInputAmount(): number {
    return this.isReversalMode()
      ? this.getTotalReversalAmount()
      : Number(this.TrxPaymentComponenRequest.InputOutstandingBalance);
  }

  getPaymentsToReverse(): TrxPaymentEntity[] {
    const paymentList: TrxPaymentEntity[] = this.TrxPaymentComponenRequest.TrxPaymentReversalList.length > 0
      ? this.TrxPaymentComponenRequest.TrxPaymentReversalList
      : this.TrxPaymentComponenRequest.TrxPaymentList;

    return paymentList.filter(e => e.TypeMovement !== 'E' && this.getPaymentReversibleAmount(e) > 0);
  }

  getVisiblePaymentList(): TrxPaymentEntity[] {
    if (!this.isReversalMode()) return this.TrxPaymentComponenRequest.TrxPaymentList;

    const paymentList: TrxPaymentEntity[] = this.TrxPaymentComponenRequest.TrxPaymentReversalList.length > 0
      ? this.TrxPaymentComponenRequest.TrxPaymentReversalList
      : this.TrxPaymentComponenRequest.TrxPaymentList.filter(e => e.TypeMovement !== 'E');

    return paymentList.reduce((result: TrxPaymentEntity[], payment: TrxPaymentEntity) => {
      const reversalList: TrxPaymentEntity[] = this.getReversalPaymentList(payment.TrxPaymentId);
      result.push(payment, ...reversalList);
      return result;
    }, []);
  }

  getReversalLimit(): number {
    const pendingRequestedAmount: number = this.toMoney(this.getTotalReversalAmount() - this.getReturnedAmount());
    const availableAmount: number = this.getPaymentsToReverse()
      .reduce((sum, e) => sum + this.getPaymentReversibleAmount(e), 0);

    return Math.min(pendingRequestedAmount, this.toMoney(availableAmount));
  }

  getTotalReversalAmount(): number {
    const inputAmount: number = Number(this.TrxPaymentComponenRequest.InputReversalAmount);
    const originalAmount: number = (this.TrxPaymentComponenRequest.TrxPaymentReversalList ?? [])
      .reduce((sum, e) => sum + Math.abs(Number(e.AmountPaid || 0)), 0);

    if (inputAmount > 0) return this.toMoney(inputAmount);

    return this.toMoney(originalAmount);
  }

  getReturnedAmount(): number {
    return this.toMoney((this.TrxPaymentComponenRequest.TrxPaymentList ?? [])
      .filter(e => e.TypeMovement === 'E')
      .reduce((sum, e) => sum + Math.abs(Number(e.AmountPaid || 0)), 0));
  }

  getCurrentReversalAmount(): number {
    const payment: TrxPaymentEntity | undefined = this.getPaymentsToReverse()[0];

    if (!payment) return 0;

    return this.toMoney(Math.min(this.getPaymentReversibleAmount(payment), this.getReversalLimit()));
  }

  getPaymentReversibleAmount(trxPayment: TrxPaymentEntity): number {
    const paymentAmount: number = Math.abs(Number(trxPayment.AmountPaid || 0));
    const reversedAmount: number = (this.TrxPaymentComponenRequest.TrxPaymentList ?? [])
      .filter(e => e.TypeMovement === 'E' && Number(e.ReversalOfTrxPaymentId || 0) === Number(trxPayment.TrxPaymentId || 0))
      .reduce((sum, e) => sum + Math.abs(Number(e.AmountPaid || 0)), 0);

    return this.toMoney(paymentAmount - reversedAmount);
  }

  getOriginalPayment(TrxPaymentId: number | null): TrxPaymentEntity | undefined {
    return (this.TrxPaymentComponenRequest.TrxPaymentReversalList ?? [])
      .find(e => Number(e.TrxPaymentId || 0) === Number(TrxPaymentId || 0));
  }

  getReversalPaymentList(TrxPaymentId: number): TrxPaymentEntity[] {
    return (this.TrxPaymentComponenRequest.TrxPaymentList ?? [])
      .filter(e => e.TypeMovement === 'E' && Number(e.ReversalOfTrxPaymentId || 0) === Number(TrxPaymentId || 0));
  }

  isPaymentFullyReversed(trxPayment: TrxPaymentEntity): boolean {
    return trxPayment.TypeMovement !== 'E' && this.getPaymentReversibleAmount(trxPayment) <= 0;
  }

  isPaymentPartiallyReversed(trxPayment: TrxPaymentEntity): boolean {
    if (trxPayment.TypeMovement === 'E') return false;

    const reversedAmount: number = this.getReversalPaymentList(trxPayment.TrxPaymentId)
      .reduce((sum, e) => sum + Math.abs(Number(e.AmountPaid || 0)), 0);

    return reversedAmount > 0 && this.getPaymentReversibleAmount(trxPayment) > 0;
  }

  getPaymentStatusLabel(trxPayment: TrxPaymentEntity): string {
    if (!this.isReversalMode()) return "";
    if (trxPayment.TypeMovement === 'E') return "Reversion";
    if (this.isPaymentFullyReversed(trxPayment)) return "Anulado";
    if (this.isPaymentPartiallyReversed(trxPayment)) return "Parcial";
    return "Pendiente";
  }

  getPaymentMethodIconClass(trxPayment: TrxPaymentEntity): string {
    if (this.isCardPayment(trxPayment)) return "fa fa-credit-card";
    if (trxPayment.PaymentPlatform === "FISICO") return "fa fa-coins";
    return "fa fa-money-check-alt";
  }

  getPaymentMethodIconClassByCod(PaymentMethodCod: string): string {
    const paymentMethod: PaymentMethodEntity | undefined = this.getPaymentMethod(PaymentMethodCod);

    if (paymentMethod?.PaymentMethodType === "1002" || paymentMethod?.PaymentMethodType === "1003") return "fa fa-credit-card";
    if (paymentMethod?.PaymentMethodType === "1001") return "fa fa-coins";
    return "fa fa-money-check-alt";
  }

  getPaymentMethodRoute(PaymentMethodCod: string): string {
    return this.getPaymentMethod(PaymentMethodCod)?.Route || "";
  }

  hasPaymentMethodRoute(PaymentMethodCod: string): boolean {
    return this.getPaymentMethodRoute(PaymentMethodCod) !== "";
  }

  getPaymentMethodAlertMedia(trxPayment: TrxPaymentEntity): string {
    const route: string = this.getPaymentMethodRoute(trxPayment.PaymentMethodCod);

    if (route) {
      return `<img src="${route}" alt="Medio de pago" style="width: 65px; height: 65px; object-fit: contain;">`;
    }

    return `<i class="${this.getPaymentMethodIconClass(trxPayment)} text-primary" style="font-size: 2rem;"></i>`;
  }

  isCardPayment(trxPayment: TrxPaymentEntity): boolean {
    const paymentMethod: PaymentMethodEntity | undefined = this.paymentMethodList.find(e => e.PaymentMethodCod === trxPayment.PaymentMethodCod);

    if (paymentMethod) return this.IsCard(paymentMethod);

    return trxPayment.PaymentPlatform === "POS";
  }

  toMoney(value: number): number {
    return Math.round(Number(value || 0) * 100) / 100;
  }

  async FindByDocumentCod(){

    let DocumentCod : string = this.txtDocumentCod.nativeElement.value;

    const rpt : ResponseWsDto = await this.creditNoteService.FindByDocumentCod(DocumentCod);

    if(!rpt.ErrorStatus){

      this.creditNoteDetail = rpt.Data;

      this.txtAmountPaid.nativeElement.value = String(this.creditNoteDetail.Headboard.NumTotalPrice);
      this.cboCurrencyCod.nativeElement.value = this.creditNoteDetail.Headboard.CurrencyCod;
      
    }

  }

  getPaymentDescription(PaymentMethodCod : string){
    const paymentMethod: PaymentMethodEntity | undefined = this.getPaymentMethod(PaymentMethodCod);
    return paymentMethod?.Description || paymentMethod?.Name || PaymentMethodCod;
  }

  getPaymentMethod(PaymentMethodCod : string): PaymentMethodEntity | undefined {
    return this.paymentMethodList.find( e => e.PaymentMethodCod === PaymentMethodCod);
  }
  

}
