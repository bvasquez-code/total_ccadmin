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
  DocumentType: string = "03";
  enableButtonPay: boolean = false;
  ShowClientRegister: boolean = false;
  ShowClient: boolean = false;
  ShowClientSearch: boolean = false;
  ClientDocumentType: string = "";
  ClientDocumentNum: string = "";

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

    const salePayment: SalePaymentRegisterDto = new SalePaymentRegisterDto();

    salePayment.SaleCod = this.SaleDetail.Headboard.SaleCod;
    salePayment.TrxPaymentId = TrxPayment.TrxPaymentId;
    salePayment.DocumentType = this.DocumentType;

    const rpt: ResponseWsDto = await this.saleservice.AddPayment(salePayment);

    if (!rpt.ErrorStatus) {
      await this.findDataForm(this.SaleCod);

      if (this.SaleDetail.Headboard.IsPaid == "S") {

        const SaleConfirm : SaleConfirmDto = new SaleConfirmDto();
        SaleConfirm.SaleCod = this.SaleDetail.Headboard.SaleCod;
        SaleConfirm.CounterfoilCod = "";
        SaleConfirm.DocumentType = this.DocumentType;

        const rptConfirm = await this.saleservice.confirm(SaleConfirm);

        if (!rptConfirm.ErrorStatus) {

          this.SaleDetail = rptConfirm.Data;

          if(this.SaleDetail.Headboard.SaleStatus === "C"){
            this.print();
          }

        }
        
      }

    }

  }

  selectDocumentType(DocumentType: string) {
    this.DocumentType = DocumentType;
    this.refreshPaymentAvailability();

    if (this.requiresClientForSelectedDocument()) {
      if (this.hasClient() && !this.isCurrentClientCompatible()) {
        this.toastrService.error("El cliente seleccionado no corresponde al tipo de documento de venta.");
      }
      this.OpenClientModal();
    }
  }



  OpenTrxPaymentModal() {
    if (this.requiresClientForSelectedDocument()) {
      this.OpenClientModal();
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

    await this.ticketSvc.printSalesInvoice(this.SaleDetailPrintData);
  }

  viewAlertSelectDocumentType() {
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

  hasClient(): boolean {
    return this.SaleDetail.Headboard.ClientCod !== "" && this.SaleDetail.Headboard.ClientCod !== null && this.SaleDetail.Headboard.ClientCod !== undefined;
  }

  requiresClientForSelectedDocument(): boolean {
    if (this.hasClient() && !this.isCurrentClientCompatible()) return true;
    if (this.DocumentType === "01") return !this.hasClient();
    if (this.DocumentType === "03" && this.SaleDetail.Headboard.NumTotalPrice > 700) return !this.hasClient();
    return false;
  }

  isCurrentClientCompatible(): boolean {
    const person = this.SaleDetail.Headboard.Client?.Person;

    if (!person) return false;

    if (this.DocumentType === "01") {
      return person.PersonType === "04";
    }

    if (this.DocumentType === "03") {
      return true;
    }

    return true;
  }

  refreshPaymentAvailability(): void {
    this.enableButtonPay = (this.DocumentType === "01" || this.DocumentType === "03") && !this.requiresClientForSelectedDocument();
  }

  OpenClientModal() {
    this.ShowClient = false;
    this.ShowClientRegister = false;
    this.ShowClientSearch = true;
    this.ClientDocumentNum = "";
    this.ClientDocumentType = this.DocumentType === "01" ? "06" : "01";
    setTimeout(() => { this.btnOpenClientModal?.nativeElement.click(); }, 0);
  }

  async findByDocumentNum() {
    this.ClientDocumentType = this.cboDocumentType.nativeElement.value;
    this.ClientDocumentNum = this.txtDocumentNum.nativeElement.value;

    if (this.DocumentType === "01" && this.ClientDocumentType !== "06") {
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
      this.toastrService.success("Cliente asociado a la venta.");
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
