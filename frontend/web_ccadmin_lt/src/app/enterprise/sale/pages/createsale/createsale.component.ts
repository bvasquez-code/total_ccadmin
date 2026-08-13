import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { SaleService } from '../../service/sale.service';
import { Router } from '@angular/router';
import { SaleDetailDto } from '../../model/dto/SaleDetailDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { SalePaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/SalePaymentEntity';
import { TrxPaymentEntity } from 'src/app/enterprise/trxpayment/model/entity/TrxPaymentEntity';
import { SalePaymentRegisterDto } from '../../model/dto/SalePaymentRegisterDto';
import { TrxPaymentComponenRequestDto } from 'src/app/enterprise/trxpayment/model/dto/TrxPaymentComponenRequestDto';
import { TicketSunatService } from '../../service/TicketSunatService';
import { ToastrService } from 'ngx-toastr';
import { ClientService } from '../../../client/service/client.service';
import { ClientEntity } from '../../../client/model/entity/ClientEntity';
import { SaleConfirmDto } from '../../model/dto/SaleConfirmDto';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { SaleDetTaxEntity } from '../../model/entity/SaleDetTaxEntity';
import { SaleDetEntity } from '../../model/entity/SaleDetEntity';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { SalePickingLineDto } from '../../model/dto/SalePickingLineDto';
import { SalePickingConfirmDto } from '../../model/dto/SalePickingConfirmDto';
import { IndicatorDto } from 'src/app/enterprise/shared/model/dto/IndicatorDto';
import { SaleBillingEntity } from '../../model/entity/SaleBillingEntity';
import { PersonEntity } from 'src/app/enterprise/person/model/entity/PersonEntity';
import { PersonIdentityLookupService } from 'src/app/enterprise/person/service/person-identity-lookup.service';

@Component({
  selector: 'app-createsale',
  templateUrl: './createsale.component.html'
})
export class CreatesaleComponent implements OnInit {

  @ViewChild('txtDocumentNum', { static: false }) txtDocumentNum!: ElementRef<HTMLInputElement>;
  @ViewChild('cboDocumentType', { static: false }) cboDocumentType!: ElementRef<HTMLSelectElement>;
  @ViewChild('btnOpenClientModal', { static: false }) btnOpenClientModal!: ElementRef<HTMLButtonElement>;
  @ViewChild('txtPickingNumUnit', { static: false }) txtPickingNumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtPickingLotNumber', { static: false }) txtPickingLotNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtPickingExpirationDate', { static: false }) txtPickingExpirationDate!: ElementRef<HTMLInputElement>;
  @ViewChild('btnClosePickingModal', { static: false }) btnClosePickingModal!: ElementRef<HTMLButtonElement>;

  SaleCod: string = "";
  SaleDetail: SaleDetailDto = new SaleDetailDto();
  PaymentMethodList: PaymentMethodEntity[] = [];
  IndProformaSales: IndicatorDto = new IndicatorDto();
  IndAdvancePayment: IndicatorDto = new IndicatorDto();
  IndMandatoryPicking: IndicatorDto = new IndicatorDto();
  TrxPaymentList: TrxPaymentEntity[] = [];
  ItemCount: number = 0;
  SaleDetailPrintData: ResponseWsDto = new ResponseWsDto();

  TrxPaymentComponenRequest: TrxPaymentComponenRequestDto = new TrxPaymentComponenRequestDto();
  DocumentType: string = "";
  SelectedPaymentOption: string = "";
  enableButtonPay: boolean = false;
  ShowClientRegister: boolean = false;
  ShowClient: boolean = false;
  ShowClientSearch: boolean = false;
  ClientDocumentType: string = "";
  ClientDocumentNum: string = "";
  ClientSearchMode: string = "sale";
  SelectedPickingDetail: SaleDetEntity = new SaleDetEntity();
  PickingLineList: SalePickingLineDto[] = [];
  PickingDraftByItem: { [itemNumber: number]: SalePickingLineDto[] } = {};
  IsPickingDraftStarted: boolean = false;
  IsConfirmingPicking: boolean = false;
  readonly MaxLotNumberLength: number = 32;
  ReturnUrl: string = '/enterprise/sale/pages/listsale';

  constructor(
    private saleservice: SaleService
    , private router: Router
    , private ticketSvc: TicketSunatService
    , private toastrService: ToastrService
    , private clientService: ClientService
    , private personIdentityLookupService: PersonIdentityLookupService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.SaleCod = urlTree.queryParams['SaleCod'];
    const requestedReturnUrl = urlTree.queryParams['ReturnUrl'];
    if (requestedReturnUrl === '/enterprise/sale/pages/listsaleweb') {
      this.ReturnUrl = requestedReturnUrl;
    }

  }
  ngOnInit(): void {
    // setTimeout(() => {this.findDataForm(this.SaleCod);}, 100);
    this.findDataForm(this.SaleCod);
  }

  async findDataForm(SaleCod: string) {
    const rpt: ResponseWsDto = await this.saleservice.findDataForm(SaleCod);

    if (!rpt.ErrorStatus) {
      this.SaleDetail = rpt.DataAdditional.find(e => e.Name == "SaleDetail")?.Data;
      this.PaymentMethodList = rpt.DataAdditional.find(e => e.Name == "PaymentMethodList")?.Data ?? [];
      this.IndProformaSales = rpt.DataAdditional.find(e => e.Name === "IndProformaSales")?.Data ?? new IndicatorDto();
      this.IndAdvancePayment = rpt.DataAdditional.find(e => e.Name === "IndAdvancePayment")?.Data ?? new IndicatorDto();
      this.IndMandatoryPicking = rpt.DataAdditional.find(e => e.Name === "IndMandatoryPicking")?.Data ?? new IndicatorDto();

      const documentTypeRequest = this.SaleDetail.SaleBilling?.DocumentTypeRequest || "";
      if (this.isWebSale && (documentTypeRequest === "01" || documentTypeRequest === "03")) {
        // En una venta web la solicitud del cliente es la fuente autoritativa.
        // Se restablece siempre para evitar que un estado previo del componente
        // deje otro botón seleccionado después de refrescar la venta.
        this.SelectedPaymentOption = documentTypeRequest;
        this.DocumentType = documentTypeRequest;
      } else if ((documentTypeRequest === "01" || documentTypeRequest === "03")
        && !this.SelectedPaymentOption) {
        this.SelectedPaymentOption = documentTypeRequest;
        this.DocumentType = documentTypeRequest;
      }

      if (!this.isProformaSalesEnabled && this.SelectedPaymentOption === "99") {
        this.SelectedPaymentOption = "";
        this.DocumentType = "";
      }
      if (!this.isAdvancePaymentEnabled && this.SelectedPaymentOption === "advance") {
        this.SelectedPaymentOption = "";
        this.DocumentType = "";
      }

      if (this.SaleDetail.Headboard.SaleStatus === "C") {
        await this.router.navigate(
          ['/enterprise/sale/pages/viewsale'],
          {
            queryParams: {
              SaleCod: this.SaleDetail.Headboard.SaleCod,
              ReturnUrl: this.ReturnUrl
            }
          }
        );
        return;
      }

      this.TrxPaymentComponenRequest.InputOutstandingBalance = this.getOutstandingbalance();
      this.TrxPaymentComponenRequest.TrxPaymentList = this.getTrxPaymentList();
      if (this.isPickingConfirmed()) {
        this.PickingDraftByItem = {};
        this.IsPickingDraftStarted = false;
        sessionStorage.removeItem(this.getPickingStorageKey());
      } else if (!this.IsPickingDraftStarted) {
        this.restorePickingDraft();
      }
      this.refreshPaymentAvailability();
    }
  }

  async findDataPrint(SaleCod: string) {
    const rpt: ResponseWsDto = await this.saleservice.findDataPrint(SaleCod);
    this.SaleDetailPrintData = rpt;
  }

  getItemCount(): number {
    this.ItemCount++;
    return this.ItemCount;
  }

  ResponseResultFormClient(event: any) {

    const TrxPayment: TrxPaymentEntity = event;

    this.TrxPaymentList.push(TrxPayment);

    console.log(TrxPayment);

    this.AddPayment(TrxPayment);

  }

  async AddPayment(TrxPayment: TrxPaymentEntity) {
    if (!this.ensurePickingAllowsOtherActions()) return;
    if (!this.hasSelectedPaymentOption()) {
      this.toastrService.info("Seleccione un tipo de documento de venta o anticipo antes de pagar.", "Info");
      return;
    }

    const salePayment: SalePaymentRegisterDto = new SalePaymentRegisterDto();

    salePayment.SaleCod = this.SaleDetail.Headboard.SaleCod;
    salePayment.TrxPaymentId = TrxPayment.TrxPaymentId;
    salePayment.DocumentType = this.DocumentType;

    const rpt: ResponseWsDto = await this.saleservice.AddPayment(salePayment);

    if (!rpt.ErrorStatus) {
      await this.findDataForm(this.SaleCod);

      if (this.SaleDetail.Headboard.IsPaid == "S") {

        if (this.SelectedPaymentOption === "advance") {
          this.toastrService.info("Pago total registrado. Seleccione el documento de venta para cerrar la venta.", "Info");
          this.refreshPaymentAvailability();
          return;
        }

        await this.confirmSaleDocument();
      }

    }

  }

  async selectDocumentType(DocumentType: string): Promise<void> {
    if (!this.ensurePickingAllowsOtherActions()) return;
    if (this.isWebSale) {
      const requestedDocumentType = this.SaleDetail.SaleBilling?.DocumentTypeRequest || "";
      if (requestedDocumentType !== "01" && requestedDocumentType !== "03") {
        this.toastrService.error("La venta web no tiene un comprobante solicitado válido.");
        return;
      }
      this.SelectedPaymentOption = requestedDocumentType;
      this.DocumentType = requestedDocumentType;
      this.refreshPaymentAvailability();
      if (DocumentType !== requestedDocumentType) {
        this.toastrService.info("El comprobante solicitado por el cliente no puede modificarse.");
      }
      return;
    }
    if (DocumentType === "99" && !this.isProformaSalesEnabled) {
      this.toastrService.warning("La emision de proformas no esta habilitada para esta empresa.");
      return;
    }
    this.ClientSearchMode = "sale";
    this.SelectedPaymentOption = DocumentType;
    this.DocumentType = DocumentType;
    if (DocumentType === "03") {
      await this.saveBillingForBuyer("03");
    } else if (DocumentType === "01" && !this.isCurrentBillingCompatible()) {
      this.OpenClientModal("billing");
    } else if (DocumentType === "99") {
      await this.clearBillingRequest();
    }
    this.refreshPaymentAvailability();
  }

  OpenAdvanceClientModal() {
    if (!this.ensurePickingAllowsOtherActions()) return;
    if (!this.isAdvancePaymentEnabled) {
      this.toastrService.warning("El registro de anticipos no esta habilitado para esta empresa.");
      return;
    }
    this.SelectedPaymentOption = "advance";
    this.DocumentType = "03";
    this.refreshPaymentAvailability();
    if (this.hasRegisteredPayment()) {
      this.toastrService.info("La venta ya tiene un anticipo registrado. Para continuar seleccione boleta o factura.", "Info");
      return;
    }
    this.toastrService.info("El DNI solo registra a quien realizara el pago parcial. La venta podra emitirse luego como boleta o factura.", "Anticipo");
    this.OpenClientModal("advance");
  }



  async OpenTrxPaymentModal() {
    if (!this.ensurePickingAllowsOtherActions()) return;
    if (!this.hasSelectedPaymentOption()) {
      this.toastrService.info("Seleccione un tipo de documento de venta o anticipo antes de pagar.", "Info");
      return;
    }

    if (this.SelectedPaymentOption === "advance" && this.hasRegisteredPayment()) {
      this.toastrService.info("La venta ya tiene un anticipo registrado. Para continuar seleccione boleta o factura.", "Info");
      return;
    }

    if (this.requiresClientForSelectedDocument()) {
      this.OpenClientModal(this.DocumentType === "01" ? "billing" : "sale");
      return;
    }

    if (this.isPaidWithoutDocument() && this.isFinalDocumentSelected()) {
      await this.confirmSaleDocument();
      return;
    }

    this.TrxPaymentComponenRequest.InputOutstandingBalance = this.getOutstandingbalance();
  }

  getOutstandingbalance(): number {
    const totalPrice: number = Number(this.SaleDetail.Headboard.NumTotalPrice || 0);
    return Math.max(0, this.toMoney(totalPrice - this.getTotalPaid()));
  }

  hasOutstandingBalance(): boolean {
    return this.getOutstandingbalance() > 0;
  }

  getPaymentActionLabel(): string {
    return this.hasOutstandingBalance() ? "Monto por pagar" : "Facturar";
  }

  getTrxPaymentList(): TrxPaymentEntity[] {
    return this.SaleDetail.DetailPayment.map(e => e.TrxPayment);
  }

  async print() {
    if (!this.ensurePickingAllowsOtherActions()) return;

    await this.findDataPrint(this.SaleCod);

    if (this.shouldPrintAdvance(this.SaleDetailPrintData)) {
      await this.ticketSvc.printSaleAdvance(this.SaleDetailPrintData);
      return;
    }

    await this.ticketSvc.printSaleDocument(this.SaleDetailPrintData);
  }

  shouldPrintAdvance(saleDetailPrint: ResponseWsDto): boolean {
    const saleDetail: SaleDetailDto = saleDetailPrint?.DataAdditional?.find((e: any) => e.Name === "SaleDetail")?.Data;
    const documentCod = (saleDetail?.SaleDocument?.DocumentCod ?? "").toString().trim();
    const totalPaid = (saleDetail?.DetailPayment ?? [])
      .filter(e => e?.TrxPayment?.TypeMovement === "I" || !e?.TrxPayment)
      .reduce((sum, e) => sum + Number(e?.NumAmountPaid || 0), 0);

    return saleDetail?.Headboard?.SaleStatus !== "C" && documentCod === "" && totalPaid > 0;
  }

  viewAlertSelectDocumentType() {
    if (!this.ensurePickingAllowsOtherActions()) return;
    if (!this.hasSelectedPaymentOption()) {
      this.toastrService.info("Seleccione un tipo de documento de venta o anticipo antes de pagar.", "Info");
      return;
    }

    if (this.SelectedPaymentOption === "advance" && this.hasRegisteredPayment()) {
      this.toastrService.info("La venta ya tiene un anticipo registrado. Para continuar seleccione boleta o factura.", "Info");
      return;
    }

    if (this.requiresClientForSelectedDocument()) {
      if (this.hasClient() && !this.isCurrentClientCompatible()) {
        this.toastrService.info("Debe seleccionar un cliente compatible con el documento de venta.", "Info");
      } else {
        this.toastrService.info("Debe seleccionar un cliente para continuar.", "Info");
      }
      this.OpenClientModal(this.DocumentType === "01" ? "billing" : "sale");
      return;
    }

    this.toastrService.info("Seleccione un tipo de documento de venta para continuar.", "Info");
  }

  getAmountReturned(): number {
    const totalReturned: number = this.SaleDetail.DetailPayment
      .reduce((sum, e) => sum + Number(e.NumAmountReturned || 0), 0);

    return this.toMoney(totalReturned);
  }

  getVisibleQuantity(internalQuantity: number, productUnitFactor: number): number {
    return ProductUnitHelper.toVisibleQuantity(internalQuantity, productUnitFactor);
  }

  getVisibleUnitPrice(internalUnitPrice: number, productUnitFactor: number): number {
    return ProductUnitHelper.toVisibleUnitPrice(internalUnitPrice, productUnitFactor);
  }

  getTaxDetailList(item: SaleDetEntity): SaleDetTaxEntity[] {
    return item?.TaxDetailList ?? [];
  }

  hasTaxDetail(item: SaleDetEntity): boolean {
    return this.getTaxDetailList(item).length > 0;
  }

  getTaxLineLabel(tax: SaleDetTaxEntity): string {
    const name = tax.TaxName || tax.TaxCod;
    const affectation = tax.TaxAffectationCod ? `/${tax.TaxAffectationCod}` : "";
    const rate = Number(tax.TaxRateValue || 0);
    const fixed = Number(tax.FixedUnitAmount || 0);
    if (tax.TaxCalculationType === "P" && rate > 0) {
      return `${name}${affectation} ${rate}%`;
    }
    if (tax.TaxCalculationType === "F" && fixed > 0) {
      return `${name} ${fixed}`;
    }
    return `${name}${affectation}`;
  }

  hasClient(): boolean {
    return this.SaleDetail.Headboard.ClientCod !== "" && this.SaleDetail.Headboard.ClientCod !== null && this.SaleDetail.Headboard.ClientCod !== undefined;
  }

  requiresClientForSelectedDocument(): boolean {
    if (!this.hasSelectedPaymentOption()) return false;
    if (this.SelectedPaymentOption === "advance") return !this.hasClient();
    if (this.DocumentType === "01") return !this.isCurrentBillingCompatible();
    if (this.DocumentType === "03" && this.SaleDetail.Headboard.NumTotalPrice > 700) {
      return !this.isCurrentBillingCompatible();
    }
    return false;
  }

  isCurrentClientCompatible(): boolean {
    if (this.SelectedPaymentOption !== "advance") {
      return this.isCurrentBillingCompatible();
    }
    const person = this.SaleDetail.Headboard.Client?.Person;

    if (!person) return false;

    if (this.SelectedPaymentOption === "advance") {
      return person.DocumentType === "01";
    }

    return true;
  }

  isCurrentBillingCompatible(): boolean {
    const billing = this.SaleDetail.SaleBilling;
    if (!billing || billing.DocumentTypeRequest !== this.DocumentType) return false;
    if (this.DocumentType === "01") {
      return (billing.DocumentType === "06" || billing.DocumentType === "6")
        && /^\d{11}$/.test((billing.DocumentNum || "").trim())
        && !!(billing.LegalName || "").trim();
    }
    if (this.DocumentType === "03") {
      if (Number(this.SaleDetail.Headboard.NumTotalPrice || 0) <= 700) return true;
      return !!(billing.DocumentType || "").trim()
        && !!(billing.DocumentNum || "").trim()
        && !!(billing.LegalName || "").trim();
    }
    return false;
  }

  refreshPaymentAvailability(): void {
    const hasAllowedDocumentType = this.DocumentType === "01" || this.DocumentType === "03"
      || (this.DocumentType === "99" && this.isProformaSalesEnabled);
    const canUseAdvance = this.SelectedPaymentOption !== "advance" || !this.hasRegisteredPayment();
    this.enableButtonPay = this.hasSelectedPaymentOption() && hasAllowedDocumentType && canUseAdvance
      && !this.requiresClientForSelectedDocument() && !this.isPickingDraftBlocked();
  }

  hasSelectedPaymentOption(): boolean {
    return this.SelectedPaymentOption === "01" || this.SelectedPaymentOption === "03"
      || (this.SelectedPaymentOption === "99" && this.isProformaSalesEnabled)
      || (this.SelectedPaymentOption === "advance" && this.isAdvancePaymentEnabled);
  }

  isFinalDocumentSelected(): boolean {
    return this.SelectedPaymentOption === "01" || this.SelectedPaymentOption === "03"
      || (this.SelectedPaymentOption === "99" && this.isProformaSalesEnabled);
  }

  get isProformaSalesEnabled(): boolean {
    return this.IndProformaSales?.Indicator === "IND_PROFORMA_SALES"
      && (this.IndProformaSales?.Value || "N").trim().toUpperCase() === "S";
  }

  get isAdvancePaymentEnabled(): boolean {
    return this.IndAdvancePayment?.Indicator === "IND_ADVANCE_PAYMENT"
      && (this.IndAdvancePayment?.Value || "N").trim().toUpperCase() === "S";
  }

  get isWebSale(): boolean {
    return this.SaleDetail?.SaleChannel?.ChannelCod === 'WEB';
  }

  isWebDocumentLocked(documentType: string): boolean {
    return this.isWebSale
      && this.SaleDetail?.SaleBilling?.DocumentTypeRequest !== documentType;
  }

  get isMandatoryPickingEnabled(): boolean {
    return this.IndMandatoryPicking?.Indicator === "IND_MANDATORY_PICKING"
      && (this.IndMandatoryPicking?.Value || "N").trim().toUpperCase() === "S";
  }

  hasRegisteredPayment(): boolean {
    return this.getTotalPaid() > 0;
  }

  getTotalPaid(): number {
    const totalPaid: number = this.SaleDetail.DetailPayment
      .filter(e => e?.TrxPayment?.TypeMovement === "I" || !e?.TrxPayment)
      .reduce((sum, e) => sum + Number(e?.NumAmountPaid || 0), 0);

    return this.toMoney(totalPaid);
  }

  toMoney(value: number): number {
    return Math.round(Number(value || 0) * 100) / 100;
  }

  hasOfficialDocument(): boolean {
    return (this.SaleDetail?.SaleDocument?.DocumentCod ?? "").toString().trim() !== "";
  }

  isPaidWithoutDocument(): boolean {
    return this.SaleDetail.Headboard.SaleStatus !== "C" && !this.hasOfficialDocument() && (this.SaleDetail.Headboard.IsPaid === "S" || this.getOutstandingbalance() <= 0);
  }

  shouldOpenPaymentModal(): boolean {
    return !this.isPaidWithoutDocument() || !this.isFinalDocumentSelected();
  }

  async confirmSaleDocument(): Promise<void> {
    if (!this.ensurePickingAllowsOtherActions()) return;
    if (!this.isFinalDocumentSelected()) {
      this.toastrService.info("Seleccione el documento de venta para cerrar la venta.", "Info");
      return;
    }

    const SaleConfirm : SaleConfirmDto = new SaleConfirmDto();
    SaleConfirm.SaleCod = this.SaleDetail.Headboard.SaleCod;
    SaleConfirm.CounterfoilCod = "";
    SaleConfirm.DocumentType = this.DocumentType;

    const rptConfirm = await this.saleservice.confirm(SaleConfirm);

    if (!rptConfirm.ErrorStatus) {
      this.SaleDetail = rptConfirm.Data;
      this.refreshPaymentAvailability();

      if(this.SaleDetail.Headboard.SaleStatus === "C"){
        await this.closePaymentModalBeforeNavigation();
        await this.router.navigate(
          ['/enterprise/sale/pages/viewsale'],
          {
            queryParams: {
              SaleCod: this.SaleDetail.Headboard.SaleCod,
              AutoPrint: 'Y',
              ReturnUrl: this.ReturnUrl
            }
          }
        );
      }
    }
  }

  private async closePaymentModalBeforeNavigation(): Promise<void> {
    const jquery = (window as any).$;
    const paymentModal = jquery?.('#TrxPaymentModal');

    if (paymentModal?.length && paymentModal.hasClass('show')) {
      await new Promise<void>(resolve => {
        let completed = false;
        const complete = () => {
          if (completed) return;
          completed = true;
          resolve();
        };

        paymentModal.one('hidden.bs.modal', complete);
        paymentModal.modal('hide');
        window.setTimeout(complete, 500);
      });
    }

    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('padding-right');
    document.querySelectorAll('.modal-backdrop').forEach(backdrop => backdrop.remove());
  }

  OpenClientModal(mode: string = "sale") {
    if (!this.ensurePickingAllowsOtherActions()) return;
    this.ClientSearchMode = mode;
    this.ShowClient = false;
    this.ShowClientRegister = false;
    this.ShowClientSearch = true;
    this.ClientDocumentNum = "";
    this.ClientDocumentType = this.ClientSearchMode === "advance" ? "01"
      : (this.ClientSearchMode === "billing" || this.DocumentType === "01" ? "06" : "01");
    setTimeout(() => { this.btnOpenClientModal?.nativeElement.click(); }, 0);
  }

  async findByDocumentNum() {
    this.ClientDocumentType = this.ClientSearchMode === "advance" ? "01" : this.cboDocumentType.nativeElement.value;
    this.ClientDocumentNum = this.txtDocumentNum.nativeElement.value;

    if (this.ClientSearchMode === "advance" && !/^\d{8}$/.test((this.ClientDocumentNum || "").trim())) {
      this.toastrService.error("Para anticipo debe ingresar un DNI valido de 8 digitos.");
      return;
    }

    if (this.ClientSearchMode !== "advance" && this.DocumentType === "01" && this.ClientDocumentType !== "06") {
      this.toastrService.error("Para factura debe seleccionar una persona con RUC.");
      return;
    }

    if (this.ClientSearchMode === "billing") {
      const identity = await this.personIdentityLookupService.findByDocument(
        this.ClientDocumentType,
        this.ClientDocumentNum
      );
      if (!identity.person) {
        this.toastrService.error("No fue posible obtener los datos de la persona indicada.");
        return;
      }
      await this.saveBillingPerson(identity.person, this.DocumentType);
      return;
    }

    const rpt: ResponseWsDto = await this.clientService.findByDocumentNum(this.ClientDocumentType, this.ClientDocumentNum);

    if (!rpt.ErrorStatus) {
      if (rpt.Data != null) {
        await this.SaveClientSale(rpt.Data);
      }
      else {
        this.ShowClientRegister = true;
        this.ShowClientSearch = false;
        this.ShowClient = false;
      }
    }
  }

  async ResponseResultFormSaleClient(event: any) {
    const client = event as ClientEntity;
    if (this.ClientSearchMode === "billing") {
      await this.saveBillingPerson(client.Person, this.DocumentType);
      return;
    }
    await this.SaveClientSale(client);
  }

  async SaveClientSale(client: ClientEntity): Promise<void> {
    if (!this.ensurePickingAllowsOtherActions()) return;
    const rpt: ResponseWsDto = await this.saleservice.saveClientSale(this.SaleDetail.Headboard.SaleCod, client.ClientCod);

    if (!rpt.ErrorStatus) {
      this.SaleDetail.Headboard.ClientCod = client.ClientCod;
      this.SaleDetail.Headboard.Client = client;
      if (this.ClientSearchMode === "sale" && this.SelectedPaymentOption === "03") {
        await this.saveBillingForBuyer("03");
      }
      this.ShowClientRegister = false;
      this.ShowClientSearch = false;
      this.ShowClient = true;
      this.refreshPaymentAvailability();
      if (this.ClientSearchMode === "advance") {
        this.toastrService.success("DNI asociado para registrar el anticipo. Ahora puede ingresar el monto parcial en Pagar.");
      } else {
        this.toastrService.success("Cliente asociado a la venta.");
      }
    }
  }

  getInfoClient(): string {
    const person = this.SaleDetail.Headboard.Client?.Person;
    if (!person) return "";
    const name = person.BusinessName || person.CommercialName || `${person.Names} ${person.LastNames}`;
    return `${person.DocumentNum} - ${name}`;
  }

  getNameClient(): string {
    const person = this.SaleDetail.Headboard.Client?.Person;
    if (!person) return "";
    const name = person.BusinessName || person.CommercialName || `${person.Names} ${person.LastNames}`;
    return `${name}`;
  }

  getBillingName(): string {
    return this.SaleDetail.SaleBilling?.LegalName || "";
  }

  getSearchResultName(): string {
    return this.ClientSearchMode === "billing"
      ? (this.SaleDetail.SaleBilling?.LegalName || "")
      : this.getNameClient();
  }

  getSearchResultDocumentNum(): string {
    return this.ClientSearchMode === "billing"
      ? (this.SaleDetail.SaleBilling?.DocumentNum || "")
      : (this.SaleDetail.Headboard.Client?.Person?.DocumentNum || "");
  }

  getSearchResultDocumentType(): string {
    return this.ClientSearchMode === "billing"
      ? (this.SaleDetail.SaleBilling?.DocumentType || "")
      : (this.SaleDetail.Headboard.Client?.Person?.DocumentType || "");
  }

  getSearchResultDocumentLabel(): string {
    const documentType = this.getSearchResultDocumentType().replace(/^0+/, "");
    if (documentType === "6") return "RUC";
    if (documentType === "1") return "DNI";
    if (documentType === "4") return "Carnet de extranjería";
    return "Documento";
  }

  getSearchResultContextLabel(): string {
    if (this.ClientSearchMode === "billing") return "Datos de facturación";
    if (this.ClientSearchMode === "advance") return "Persona del anticipo";
    return "Cliente de la venta";
  }

  getSearchResultAddress(): string {
    return this.ClientSearchMode === "billing"
      ? (this.SaleDetail.SaleBilling?.Address || "")
      : (this.SaleDetail.Headboard.Client?.Person?.Address || "");
  }

  isSearchResultCompany(): boolean {
    return this.getSearchResultDocumentType().replace(/^0+/, "") === "6";
  }

  private async saveBillingForBuyer(documentType: string): Promise<boolean> {
    const request = new SaleBillingEntity();
    request.SaleCod = this.SaleDetail.Headboard.SaleCod;
    request.DocumentTypeRequest = documentType;
    const response = await this.saleservice.saveBilling(request);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || "No se pudieron guardar los datos de facturacion.");
      return false;
    }
    this.SaleDetail.SaleBilling = response.Data;
    return true;
  }

  private async saveBillingPerson(person: PersonEntity, documentType: string): Promise<boolean> {
    const request = new SaleBillingEntity();
    request.SaleCod = this.SaleDetail.Headboard.SaleCod;
    request.DocumentTypeRequest = documentType;
    request.PersonCod = person.PersonCod || "";
    request.Person = person;
    const response = await this.saleservice.saveBilling(request);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || "No se pudieron guardar los datos de facturacion.");
      return false;
    }
    this.SaleDetail.SaleBilling = response.Data;
    this.ShowClientRegister = false;
    this.ShowClientSearch = false;
    this.ShowClient = true;
    this.refreshPaymentAvailability();
    this.toastrService.success("Datos de facturacion registrados.");
    return true;
  }

  private async clearBillingRequest(): Promise<void> {
    const request = new SaleBillingEntity();
    request.SaleCod = this.SaleDetail.Headboard.SaleCod;
    const response = await this.saleservice.saveBilling(request);
    if (!response.ErrorStatus) {
      this.SaleDetail.SaleBilling = response.Data;
    }
  }

  openPickingModal(item: SaleDetEntity): void {
    this.SelectedPickingDetail = item;
    const sourceList = this.isPickingConfirmed()
      ? (item.DetailWarehouse ?? []).map(detail => ({
          ItemNumber: item.ItemNumber,
          NumUnit: Number(detail.NumUnit || 0),
          LotNumber: detail.LotNumber || "",
          ExpirationDate: detail.ExpirationDate
        } as SalePickingLineDto))
      : (this.PickingDraftByItem[item.ItemNumber] ?? []);
    this.PickingLineList = sourceList.map(line => ({ ...line } as SalePickingLineDto));
    this.clearPickingForm();
  }

  addPickingLine(): void {
    try {
      const visibleQuantity = Number(this.txtPickingNumUnit.nativeElement.value);
      const lotNumber = (this.txtPickingLotNumber.nativeElement.value || "").trim();
      const expirationDate = this.txtPickingExpirationDate.nativeElement.value || null;
      const internalQuantity = ProductUnitHelper.toInternalQuantity(
        visibleQuantity,
        this.SelectedPickingDetail.ProductUnitFactor
      );

      if (!visibleQuantity || visibleQuantity <= 0) {
        throw new Error("Ingrese una cantidad valida.");
      }
      if (!Number.isInteger(internalQuantity)) {
        throw new Error("La cantidad no es compatible con la unidad del producto.");
      }
      if (internalQuantity % ProductUnitHelper.normalizeFactor(this.SelectedPickingDetail.ProductUnitFactor) !== 0) {
        throw new Error(`Ingrese cantidades completas en ${this.SelectedPickingDetail.ProductUnitName}.`);
      }
      if (lotNumber.length > this.MaxLotNumberLength) {
        throw new Error(`El lote no puede superar ${this.MaxLotNumberLength} caracteres.`);
      }
      if (expirationDate && expirationDate < this.getTodayDateInput()) {
        throw new Error("La fecha de vencimiento no puede estar vencida.");
      }
      if (this.getPickingTotalInternal() + internalQuantity > this.SelectedPickingDetail.NumUnit) {
        throw new Error("La cantidad pickeada no puede superar la cantidad vendida.");
      }

      const existingLine = this.PickingLineList.find(line =>
        (line.LotNumber || "").trim().toUpperCase() === lotNumber.toUpperCase()
        && (line.ExpirationDate || null) === expirationDate
      );
      if (existingLine) {
        existingLine.NumUnit += internalQuantity;
      } else {
        this.PickingLineList.push({
          ItemNumber: this.SelectedPickingDetail.ItemNumber,
          NumUnit: internalQuantity,
          LotNumber: lotNumber,
          ExpirationDate: expirationDate
        } as SalePickingLineDto);
      }
      this.saveCurrentPickingDraft();
      this.clearPickingForm();
    } catch (error: any) {
      this.toastrService.error(error.message);
    }
  }

  removePickingLine(index: number): void {
    if (this.isPickingConfirmed()) return;
    this.PickingLineList.splice(index, 1);
    this.saveCurrentPickingDraft();
  }

  confirmProductPicking(): void {
    try {
      if (this.PickingLineList.length === 0) {
        throw new Error("Debe agregar al menos un lote.");
      }
      if (this.getPickingTotalInternal() !== Number(this.SelectedPickingDetail.NumUnit || 0)) {
        throw new Error("La cantidad pickeada debe ser exactamente igual a la cantidad vendida.");
      }
      this.PickingDraftByItem[this.SelectedPickingDetail.ItemNumber] =
        this.PickingLineList.map(line => ({ ...line } as SalePickingLineDto));
      this.IsPickingDraftStarted = true;
      this.persistPickingDraft();
      this.refreshPaymentAvailability();
      this.btnClosePickingModal.nativeElement.click();
    } catch (error: any) {
      this.toastrService.error(error.message);
    }
  }

  async confirmAllPicking(): Promise<void> {
    if (this.IsConfirmingPicking || this.isPickingConfirmed()) return;
    if (!this.canConfirmPickingDraft()) {
      const message = this.isMandatoryPickingEnabled
        ? "Debe pickear la cantidad exacta de todos los productos."
        : "Debe completar el pickeo de al menos un producto.";
      this.toastrService.error(message);
      return;
    }

    const request = new SalePickingConfirmDto();
    request.SaleCod = this.SaleDetail.Headboard.SaleCod;
    request.DetailList = this.SaleDetail.DetailList.flatMap(item =>
      (this.PickingDraftByItem[item.ItemNumber] ?? []).map(line => ({ ...line } as SalePickingLineDto))
    );

    this.IsConfirmingPicking = true;
    try {
      const response = await this.saleservice.confirmPicking(request);
      if (!response.ErrorStatus) {
        this.SaleDetail = response.Data;
        this.PickingDraftByItem = {};
        this.IsPickingDraftStarted = false;
        sessionStorage.removeItem(this.getPickingStorageKey());
        this.refreshPaymentAvailability();
        this.toastrService.success("Pickeo confirmado correctamente.");
      }
    } finally {
      this.IsConfirmingPicking = false;
    }
  }

  getPickingTotalInternal(): number {
    return this.PickingLineList.reduce((total, line) => total + Number(line.NumUnit || 0), 0);
  }

  getPickingTotalVisible(): number {
    return this.getVisibleQuantity(
      this.getPickingTotalInternal(),
      this.SelectedPickingDetail.ProductUnitFactor
    );
  }

  getPickingPendingVisible(): number {
    return this.getVisibleQuantity(
      Number(this.SelectedPickingDetail.NumUnit || 0) - this.getPickingTotalInternal(),
      this.SelectedPickingDetail.ProductUnitFactor
    );
  }

  getPickingLineVisibleQuantity(line: SalePickingLineDto): number {
    return this.getVisibleQuantity(line.NumUnit, this.SelectedPickingDetail.ProductUnitFactor);
  }

  getPickedQuantityVisible(item: SaleDetEntity): number {
    const sourceList = this.isPickingConfirmed()
      ? (item.DetailWarehouse ?? [])
      : (this.PickingDraftByItem[item.ItemNumber] ?? []);
    const total = sourceList.reduce((sum, line) => sum + Number(line.NumUnit || 0), 0);
    return this.getVisibleQuantity(total, item.ProductUnitFactor);
  }

  isProductPickingComplete(item: SaleDetEntity): boolean {
    if (this.isPickingConfirmed()) return true;
    const lineList = this.PickingDraftByItem[item.ItemNumber] ?? [];
    return lineList.length > 0
      && lineList.reduce((sum, line) => sum + Number(line.NumUnit || 0), 0) === Number(item.NumUnit || 0);
  }

  canConfirmPickingDraft(): boolean {
    const startedProductList = this.SaleDetail.DetailList.filter(item =>
      (this.PickingDraftByItem[item.ItemNumber] ?? []).length > 0
    );
    if (startedProductList.length === 0
      || startedProductList.some(item => !this.isProductPickingComplete(item))) {
      return false;
    }
    return !this.isMandatoryPickingEnabled
      || this.SaleDetail.DetailList.every(item => this.isProductPickingComplete(item));
  }

  isPickingConfirmed(): boolean {
    return this.SaleDetail?.Headboard?.IsPickingConfirmed === "S";
  }

  isPickingDraftBlocked(): boolean {
    return !this.isPickingConfirmed()
      && (this.IsPickingDraftStarted || this.isMandatoryPickingEnabled);
  }

  ensurePickingAllowsOtherActions(): boolean {
    if (!this.isPickingDraftBlocked()) return true;
    const message = this.isMandatoryPickingEnabled
      ? "Debe completar y confirmar el pickeo de todos los productos antes de realizar otra operacion."
      : "Debe confirmar el pickeo iniciado antes de realizar otra operacion.";
    this.toastrService.warning(message);
    return false;
  }

  goBack(event: Event): void {
    event.preventDefault();
    if (!this.ensurePickingAllowsOtherActions()) return;
    this.router.navigate([this.ReturnUrl]);
  }

  getTodayDateInput(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = `${today.getMonth() + 1}`.padStart(2, "0");
    const day = `${today.getDate()}`.padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  private clearPickingForm(): void {
    setTimeout(() => {
      if (this.txtPickingNumUnit) this.txtPickingNumUnit.nativeElement.value = "";
      if (this.txtPickingLotNumber) this.txtPickingLotNumber.nativeElement.value = "";
      if (this.txtPickingExpirationDate) this.txtPickingExpirationDate.nativeElement.value = "";
    });
  }

  private saveCurrentPickingDraft(): void {
    if (this.PickingLineList.length > 0) {
      this.PickingDraftByItem[this.SelectedPickingDetail.ItemNumber] =
        this.PickingLineList.map(line => ({ ...line } as SalePickingLineDto));
    } else {
      delete this.PickingDraftByItem[this.SelectedPickingDetail.ItemNumber];
    }
    this.IsPickingDraftStarted = Object.keys(this.PickingDraftByItem).length > 0;
    this.persistPickingDraft();
    this.refreshPaymentAvailability();
  }

  private persistPickingDraft(): void {
    if (Object.keys(this.PickingDraftByItem).length === 0) {
      sessionStorage.removeItem(this.getPickingStorageKey());
      return;
    }
    sessionStorage.setItem(this.getPickingStorageKey(), JSON.stringify(this.PickingDraftByItem));
  }

  private restorePickingDraft(): void {
    const savedDraft = sessionStorage.getItem(this.getPickingStorageKey());
    if (!savedDraft) return;
    try {
      const parsedDraft = JSON.parse(savedDraft);
      if (parsedDraft && typeof parsedDraft === "object") {
        this.PickingDraftByItem = parsedDraft;
        this.IsPickingDraftStarted = Object.keys(parsedDraft).length > 0;
      }
    } catch {
      sessionStorage.removeItem(this.getPickingStorageKey());
    }
  }

  private getPickingStorageKey(): string {
    return `sale-picking-draft-${this.SaleCod}`;
  }
}
