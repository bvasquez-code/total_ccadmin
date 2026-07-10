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

@Component({
  selector: 'app-createsale',
  templateUrl: './createsale.component.html'
})
export class CreatesaleComponent implements OnInit {

  @ViewChild('txtDocumentNum', { static: false }) txtDocumentNum!: ElementRef<HTMLInputElement>;
  @ViewChild('cboDocumentType', { static: false }) cboDocumentType!: ElementRef<HTMLSelectElement>;
  @ViewChild('btnOpenClientModal', { static: false }) btnOpenClientModal!: ElementRef<HTMLButtonElement>;

  SaleCod: string = "";
  SaleDetail: SaleDetailDto = new SaleDetailDto();
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

  constructor(
    private saleservice: SaleService
    , private router: Router
    , private ticketSvc: TicketSunatService
    , private toastrService: ToastrService
    , private clientService: ClientService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.SaleCod = urlTree.queryParams['SaleCod'];

  }
  ngOnInit(): void {
    // setTimeout(() => {this.findDataForm(this.SaleCod);}, 100);
    this.findDataForm(this.SaleCod);
  }

  async findDataForm(SaleCod: string) {
    const rpt: ResponseWsDto = await this.saleservice.findDataForm(SaleCod);

    if (!rpt.ErrorStatus) {
      this.SaleDetail = rpt.DataAdditional.find(e => e.Name == "SaleDetail")?.Data;

      this.TrxPaymentComponenRequest.InputOutstandingBalance = this.getOutstandingbalance();
      this.TrxPaymentComponenRequest.TrxPaymentList = this.getTrxPaymentList();
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
    if (!this.hasSelectedPaymentOption()) {
      this.toastrService.info("Seleccione boleta, factura o anticipo antes de pagar.", "Info");
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
          this.toastrService.info("Pago total registrado. Seleccione boleta o factura para emitir el documento final.", "Info");
          this.refreshPaymentAvailability();
          return;
        }

        await this.confirmSaleDocument();
      }

    }

  }

  selectDocumentType(DocumentType: string) {
    this.ClientSearchMode = "sale";
    this.SelectedPaymentOption = DocumentType;
    this.DocumentType = DocumentType;
    this.refreshPaymentAvailability();

    if (this.requiresClientForSelectedDocument()) {
      if (this.hasClient() && !this.isCurrentClientCompatible()) {
        this.toastrService.error("El cliente seleccionado no corresponde al tipo de documento de venta.");
      }
      this.OpenClientModal();
    }
  }

  OpenAdvanceClientModal() {
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
    if (!this.hasSelectedPaymentOption()) {
      this.toastrService.info("Seleccione boleta, factura o anticipo antes de pagar.", "Info");
      return;
    }

    if (this.SelectedPaymentOption === "advance" && this.hasRegisteredPayment()) {
      this.toastrService.info("La venta ya tiene un anticipo registrado. Para continuar seleccione boleta o factura.", "Info");
      return;
    }

    if (this.requiresClientForSelectedDocument()) {
      this.OpenClientModal();
      return;
    }

    if (this.isPaidWithoutDocument() && this.isFinalDocumentSelected()) {
      await this.confirmSaleDocument();
      return;
    }

    this.TrxPaymentComponenRequest.InputOutstandingBalance = this.getOutstandingbalance();
  }

  getOutstandingbalance(): number {
    return this.SaleDetail.Headboard.NumTotalPrice - this.SaleDetail.DetailPayment.reduce((sum, e) => sum + e.NumAmountPaid, 0);
  }

  getTrxPaymentList(): TrxPaymentEntity[] {
    return this.SaleDetail.DetailPayment.map(e => e.TrxPayment);
  }

  async print() {

    await this.findDataPrint(this.SaleCod);

    if (this.shouldPrintAdvance(this.SaleDetailPrintData)) {
      await this.ticketSvc.printSaleAdvance(this.SaleDetailPrintData);
      return;
    }

    await this.ticketSvc.printSalesInvoice(this.SaleDetailPrintData);
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
    if (!this.hasSelectedPaymentOption()) {
      this.toastrService.info("Seleccione boleta, factura o anticipo antes de pagar.", "Info");
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
      this.OpenClientModal();
      return;
    }

    this.toastrService.info("Seleccione un tipo de documento de venta para continuar.", "Info");
  }

  getAmountReturned(): number {
    return this.SaleDetail.DetailPayment.reduce((sum, e) => sum + (e.NumAmountReturned ? e.NumAmountReturned : 0), 0);
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
    if (this.hasClient() && !this.isCurrentClientCompatible()) return true;
    if (this.SelectedPaymentOption === "advance") return !this.hasClient();
    if (this.DocumentType === "01") return !this.hasClient();
    if (this.DocumentType === "03" && this.SaleDetail.Headboard.NumTotalPrice > 700) return !this.hasClient();
    return false;
  }

  isCurrentClientCompatible(): boolean {
    const person = this.SaleDetail.Headboard.Client?.Person;

    if (!person) return false;

    if (this.SelectedPaymentOption === "advance") {
      return person.DocumentType === "01";
    }

    if (this.DocumentType === "01") {
      return person.PersonType === "04";
    }

    if (this.DocumentType === "03") {
      return true;
    }

    return true;
  }

  refreshPaymentAvailability(): void {
    const hasAllowedDocumentType = this.DocumentType === "01" || this.DocumentType === "03";
    const canUseAdvance = this.SelectedPaymentOption !== "advance" || !this.hasRegisteredPayment();
    this.enableButtonPay = this.hasSelectedPaymentOption() && hasAllowedDocumentType && canUseAdvance && !this.requiresClientForSelectedDocument();
  }

  hasSelectedPaymentOption(): boolean {
    return this.SelectedPaymentOption === "01" || this.SelectedPaymentOption === "03" || this.SelectedPaymentOption === "advance";
  }

  isFinalDocumentSelected(): boolean {
    return this.SelectedPaymentOption === "01" || this.SelectedPaymentOption === "03";
  }

  hasRegisteredPayment(): boolean {
    return this.getTotalPaid() > 0;
  }

  getTotalPaid(): number {
    return this.SaleDetail.DetailPayment
      .filter(e => e?.TrxPayment?.TypeMovement === "I" || !e?.TrxPayment)
      .reduce((sum, e) => sum + Number(e?.NumAmountPaid || 0), 0);
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
    if (!this.isFinalDocumentSelected()) {
      this.toastrService.info("Seleccione boleta o factura para emitir el documento final.", "Info");
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
        this.print();
      }
    }
  }

  OpenClientModal(mode: string = "sale") {
    this.ClientSearchMode = mode;
    this.ShowClient = false;
    this.ShowClientRegister = false;
    this.ShowClientSearch = true;
    this.ClientDocumentNum = "";
    this.ClientDocumentType = this.ClientSearchMode === "advance" ? "01" : (this.DocumentType === "01" ? "06" : "01");
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
      this.toastrService.error("Para factura debe seleccionar un cliente con RUC.");
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
    await this.SaveClientSale(event);
  }

  async SaveClientSale(client: ClientEntity): Promise<void> {
    const rpt: ResponseWsDto = await this.saleservice.saveClientSale(this.SaleDetail.Headboard.SaleCod, client.ClientCod);

    if (!rpt.ErrorStatus) {
      this.SaleDetail.Headboard.ClientCod = client.ClientCod;
      this.SaleDetail.Headboard.Client = client;
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
}
