import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ClientEntity } from 'src/app/enterprise/client/model/entity/ClientEntity';
import { ClientService } from 'src/app/enterprise/client/service/client.service';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { SaleDetailDto } from '../../model/dto/SaleDetailDto';
import { SaleDocumentIssueDto } from '../../model/dto/SaleDocumentIssueDto';
import { SaleDetEntity } from '../../model/entity/SaleDetEntity';
import { SaleDetTaxEntity } from '../../model/entity/SaleDetTaxEntity';
import { SaleService } from '../../service/sale.service';

@Component({
  selector: 'app-createsaledocument',
  templateUrl: './createsaledocument.component.html'
})
export class CreatesaledocumentComponent implements OnInit {

  @ViewChild('txtDocumentNum', { static: false }) txtDocumentNum!: ElementRef<HTMLInputElement>;
  @ViewChild('cboDocumentType', { static: false }) cboDocumentType!: ElementRef<HTMLSelectElement>;
  @ViewChild('btnOpenClientModal', { static: false }) btnOpenClientModal!: ElementRef<HTMLButtonElement>;

  SaleCod: string = '';
  SaleDetail: SaleDetailDto = new SaleDetailDto();
  PaymentMethodList: PaymentMethodEntity[] = [];
  DocumentType: string = '';
  ClientDocumentType: string = '';
  ClientDocumentNum: string = '';
  ShowClientRegister: boolean = false;
  ShowClientSearch: boolean = false;
  ShowClient: boolean = false;
  loading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private saleService: SaleService,
    private clientService: ClientService,
    private toastrService: ToastrService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(async params => {
      this.SaleCod = params.get('SaleCod') || '';
      if (!this.SaleCod) {
        this.toastrService.error('Debe indicar la venta que desea facturar.');
        await this.router.navigate(['/enterprise/sale/pages/listsale']);
        return;
      }
      await this.findDataForm();
    });
  }

  async findDataForm(): Promise<void> {
    this.loading = true;
    try {
      const response: ResponseWsDto = await this.saleService.findDataForm(this.SaleCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudo cargar la venta.');
        return;
      }
      this.SaleDetail = response.DataAdditional.find(item => item.Name === 'SaleDetail')?.Data
        ?? new SaleDetailDto();
      this.PaymentMethodList = response.DataAdditional.find(item => item.Name === 'PaymentMethodList')?.Data
        ?? [];

      if (this.SaleDetail.Headboard.SaleStatus !== 'C') {
        this.toastrService.warning('Solo se puede facturar una venta confirmada.');
        await this.router.navigate(['/enterprise/sale/pages/listsale']);
        return;
      }
      if (this.SaleDetail.Headboard.HasFiscalDocument === 'S' || this.hasFiscalDocument()) {
        this.toastrService.info('La venta ya tiene una boleta o factura emitida.');
        await this.router.navigate(
          ['/enterprise/sale/pages/viewsale'],
          { queryParams: { SaleCod: this.SaleCod } }
        );
        return;
      }
      if (!this.hasProforma()) {
        this.toastrService.warning('La venta no tiene una proforma activa para facturar.');
        await this.router.navigate(
          ['/enterprise/sale/pages/viewsale'],
          { queryParams: { SaleCod: this.SaleCod } }
        );
      }
    } finally {
      this.loading = false;
    }
  }

  selectDocumentType(documentType: string): void {
    this.DocumentType = documentType;
    if (this.hasClient() && !this.isCurrentClientCompatible()) {
      this.toastrService.warning('Seleccione un cliente compatible con el documento elegido.');
      this.openClientModal();
      return;
    }
    if (this.requiresClient()) {
      this.openClientModal();
    }
  }

  async issueFiscalDocument(): Promise<void> {
    if (this.DocumentType !== '01' && this.DocumentType !== '03') {
      this.toastrService.info('Seleccione boleta o factura.');
      return;
    }
    if (this.requiresClient() || !this.isCurrentClientCompatible()) {
      this.toastrService.warning('Seleccione un cliente compatible antes de emitir el documento.');
      this.openClientModal();
      return;
    }

    const request = new SaleDocumentIssueDto();
    request.SaleCod = this.SaleCod;
    request.DocumentType = this.DocumentType;
    request.ClientCod = this.SaleDetail.Headboard.ClientCod || '';

    this.loading = true;
    try {
      const response: ResponseWsDto = await this.saleService.issueFiscalDocument(request);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudo emitir el documento.');
        return;
      }
      const saleDetail: SaleDetailDto = response.Data;
      const fiscalDocument = (saleDetail?.SaleDocumentList ?? [])
        .find(document => document.DocumentRole === 'F');
      this.toastrService.success('Documento fiscal emitido correctamente.');
      await this.router.navigate(
        ['/enterprise/sale/pages/viewsale'],
        {
          queryParams: {
            SaleCod: this.SaleCod,
            AutoPrint: 'Y',
            DocumentCod: fiscalDocument?.DocumentCod || saleDetail?.SaleDocument?.DocumentCod || ''
          }
        }
      );
    } finally {
      this.loading = false;
    }
  }

  openClientModal(): void {
    if (this.DocumentType !== '01' && this.DocumentType !== '03') {
      this.toastrService.info('Seleccione primero boleta o factura.');
      return;
    }
    this.ShowClient = false;
    this.ShowClientRegister = false;
    this.ShowClientSearch = true;
    this.ClientDocumentNum = '';
    this.ClientDocumentType = this.DocumentType === '01' ? '06' : '01';
    setTimeout(() => this.btnOpenClientModal?.nativeElement.click(), 0);
  }

  async findByDocumentNum(): Promise<void> {
    this.ClientDocumentType = this.cboDocumentType.nativeElement.value;
    this.ClientDocumentNum = (this.txtDocumentNum.nativeElement.value || '').trim();
    if (!this.ClientDocumentNum) {
      this.toastrService.info('Ingrese el numero de documento del cliente.');
      return;
    }
    if (this.DocumentType === '01' && this.ClientDocumentType !== '06') {
      this.toastrService.error('Para factura debe seleccionar un cliente con RUC.');
      return;
    }

    const response: ResponseWsDto = await this.clientService.findByDocumentNum(
      this.ClientDocumentType,
      this.ClientDocumentNum
    );
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudo buscar el cliente.');
      return;
    }
    if (response.Data) {
      this.selectClient(response.Data);
      return;
    }
    this.ShowClientRegister = true;
    this.ShowClientSearch = false;
    this.ShowClient = false;
  }

  responseResultFormSaleClient(event: any): void {
    this.selectClient(event as ClientEntity);
  }

  selectClient(client: ClientEntity): void {
    this.SaleDetail.Headboard.ClientCod = client.ClientCod;
    this.SaleDetail.Headboard.Client = client;
    this.ShowClientRegister = false;
    this.ShowClientSearch = false;
    this.ShowClient = true;
    this.toastrService.success('Cliente seleccionado para el nuevo documento.');
  }

  hasClient(): boolean {
    return !!this.SaleDetail?.Headboard?.ClientCod && !!this.SaleDetail?.Headboard?.Client?.Person;
  }

  requiresClient(): boolean {
    if (this.DocumentType === '01') return !this.hasClient();
    if (this.DocumentType === '03' && Number(this.SaleDetail.Headboard.NumTotalPrice || 0) > 700) {
      return !this.hasClient();
    }
    return false;
  }

  isCurrentClientCompatible(): boolean {
    if (this.DocumentType === '03') {
      if (Number(this.SaleDetail.Headboard.NumTotalPrice || 0) <= 700) return true;
      const receiptPerson = this.SaleDetail?.Headboard?.Client?.Person;
      const receiptName = receiptPerson
        ? (receiptPerson.BusinessName || receiptPerson.CommercialName
          || `${receiptPerson.Names || ''} ${receiptPerson.LastNames || ''}`.trim())
        : '';
      return !!receiptPerson?.DocumentType && !!(receiptPerson.DocumentNum || '').trim() && !!receiptName;
    }
    if (this.DocumentType !== '01') return false;
    const person = this.SaleDetail?.Headboard?.Client?.Person;
    return !!person
      && person.PersonType === '04'
      && (person.DocumentType === '06' || person.DocumentType === '6')
      && /^\d{11}$/.test((person.DocumentNum || '').trim());
  }

  getClientName(): string {
    const person = this.SaleDetail?.Headboard?.Client?.Person;
    if (!person) return 'Cliente no identificado';
    const naturalName = `${person.Names || ''} ${person.LastNames || ''}`.trim();
    return person.BusinessName || person.CommercialName || naturalName || 'Cliente no identificado';
  }

  getClientInfo(): string {
    const person = this.SaleDetail?.Headboard?.Client?.Person;
    if (!person) return '';
    return `${person.DocumentNum || ''} - ${this.getClientName()}`;
  }

  hasProforma(): boolean {
    return (this.SaleDetail?.SaleDocumentList ?? [])
      .some(document => document.DocumentType === '99' && document.Status === 'A');
  }

  hasFiscalDocument(): boolean {
    return (this.SaleDetail?.SaleDocumentList ?? [])
      .some(document => document.DocumentRole === 'F' && document.Status === 'A');
  }

  getProformaCode(): string {
    return (this.SaleDetail?.SaleDocumentList ?? [])
      .find(document => document.DocumentType === '99')?.DocumentCod || '';
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

  getTaxLineLabel(tax: SaleDetTaxEntity): string {
    const name = tax.TaxName || tax.TaxCod;
    const affectation = tax.TaxAffectationCod ? `/${tax.TaxAffectationCod}` : '';
    const rate = Number(tax.TaxRateValue || 0);
    return tax.TaxCalculationType === 'P' && rate > 0
      ? `${name}${affectation} ${rate}%`
      : `${name}${affectation}`;
  }
}
