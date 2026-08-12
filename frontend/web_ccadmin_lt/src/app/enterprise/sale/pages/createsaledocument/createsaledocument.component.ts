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
import { PersonIdentityLookupService } from 'src/app/enterprise/person/service/person-identity-lookup.service';
import { PersonEntity } from 'src/app/enterprise/person/model/entity/PersonEntity';
import { SaleBillingEntity } from '../../model/entity/SaleBillingEntity';

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
  ClientSearchMode: string = 'buyer';
  ShowClientRegister: boolean = false;
  ShowClientSearch: boolean = false;
  ShowClient: boolean = false;
  loading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private saleService: SaleService,
    private clientService: ClientService,
    private personIdentityLookupService: PersonIdentityLookupService,
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

  async selectDocumentType(documentType: string): Promise<void> {
    this.DocumentType = documentType;
    if (documentType === '03') {
      await this.saveBillingForBuyer();
      return;
    }
    if (!this.isCurrentBillingCompatible()) {
      this.openClientModal();
    }
  }

  async issueFiscalDocument(): Promise<void> {
    if (this.DocumentType !== '01' && this.DocumentType !== '03') {
      this.toastrService.info('Seleccione boleta o factura.');
      return;
    }
    if (!this.isCurrentBillingCompatible()) {
      this.toastrService.warning('Complete los datos de facturacion antes de emitir el documento.');
      this.openClientModal();
      return;
    }

    const request = new SaleDocumentIssueDto();
    request.SaleCod = this.SaleCod;
    request.DocumentType = this.DocumentType;
    request.SaleBilling = Object.assign(
      new SaleBillingEntity(),
      this.SaleDetail.SaleBilling || {}
    );
    request.SaleBilling.SaleCod = this.SaleCod;
    request.SaleBilling.DocumentTypeRequest = this.DocumentType;

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
    this.ClientSearchMode = this.DocumentType === '01' ? 'billing' : 'buyer';
    this.ShowClient = false;
    this.ShowClientRegister = false;
    this.ShowClientSearch = true;
    this.ClientDocumentNum = '';
    this.ClientDocumentType = this.ClientSearchMode === 'billing' ? '06' : '01';
    setTimeout(() => this.btnOpenClientModal?.nativeElement.click(), 0);
  }

  async findByDocumentNum(): Promise<void> {
    this.ClientDocumentType = this.cboDocumentType.nativeElement.value;
    this.ClientDocumentNum = (this.txtDocumentNum.nativeElement.value || '').trim();
    if (!this.ClientDocumentNum) {
      this.toastrService.info('Ingrese el numero de documento del cliente.');
      return;
    }
    if (this.ClientSearchMode === 'billing' && this.ClientDocumentType !== '06') {
      this.toastrService.error('Para factura debe seleccionar un cliente con RUC.');
      return;
    }

    if (this.ClientSearchMode === 'buyer') {
      const response = await this.clientService.findByDocumentNum(
        this.ClientDocumentType,
        this.ClientDocumentNum
      );
      if (response.ErrorStatus) return;
      if (!response.Data) {
        this.ShowClientRegister = true;
        this.ShowClientSearch = false;
        this.ShowClient = false;
        return;
      }
      await this.selectBuyer(response.Data as ClientEntity);
      return;
    }

    const identity = await this.personIdentityLookupService.findByDocument(
      this.ClientDocumentType,
      this.ClientDocumentNum
    );
    if (!identity.person) {
      this.toastrService.error('No fue posible obtener los datos de la persona indicada.');
      return;
    }
    await this.selectBillingPerson(identity.person);
  }

  async responseResultFormSaleClient(event: any): Promise<void> {
    await this.selectBuyer(event as ClientEntity);
  }

  private async selectBuyer(client: ClientEntity): Promise<void> {
    const response = await this.saleService.saveClientSale(this.SaleCod, client.ClientCod);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudo asociar el cliente a la venta.');
      return;
    }
    this.SaleDetail.Headboard.ClientCod = client.ClientCod;
    this.SaleDetail.Headboard.Client = client;
    await this.saveBillingForBuyer();
    this.ShowClientRegister = false;
    this.ShowClientSearch = false;
    this.ShowClient = true;
    this.toastrService.success('Cliente asociado a la boleta.');
  }

  async selectBillingPerson(person: PersonEntity): Promise<void> {
    const billing = new SaleBillingEntity();
    billing.SaleCod = this.SaleCod;
    billing.DocumentTypeRequest = this.DocumentType;
    billing.PersonCod = person.PersonCod || '';
    billing.Person = person;
    billing.DocumentType = person.DocumentType;
    billing.DocumentNum = person.DocumentNum;
    billing.LegalName = person.BusinessName || person.CommercialName
      || `${person.Names || ''} ${person.LastNames || ''}`.trim();
    billing.CommercialName = person.CommercialName;
    billing.Address = person.Address;
    billing.UbigeoCod = person.UbigeoCod;
    const response = await this.saleService.saveBilling(billing);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudieron guardar los datos de facturacion.');
      return;
    }
    this.SaleDetail.SaleBilling = response.Data;
    this.ShowClientSearch = false;
    this.ShowClient = true;
    this.toastrService.success('Persona seleccionada para el nuevo documento.');
  }

  hasClient(): boolean {
    return !!this.SaleDetail?.SaleBilling?.DocumentNum;
  }

  requiresClient(): boolean {
    return !!this.DocumentType && !this.isCurrentBillingCompatible();
  }

  isCurrentBillingCompatible(): boolean {
    if (this.DocumentType === '03') {
      const billing = this.SaleDetail?.SaleBilling;
      if (billing?.DocumentTypeRequest !== '03') return false;
      if (Number(this.SaleDetail.Headboard.NumTotalPrice || 0) <= 700) return true;
      return !!billing.DocumentType && !!(billing.DocumentNum || '').trim()
        && !!(billing.LegalName || '').trim();
    }
    if (this.DocumentType !== '01') return false;
    const billing = this.SaleDetail?.SaleBilling;
    return !!billing
      && (billing.DocumentType === '06' || billing.DocumentType === '6')
      && /^\d{11}$/.test((billing.DocumentNum || '').trim())
      && !!(billing.LegalName || '').trim();
  }

  getClientName(): string {
    return this.SaleDetail?.SaleBilling?.LegalName || 'Persona no identificada';
  }

  getClientInfo(): string {
    const billing = this.SaleDetail?.SaleBilling;
    if (!billing) return '';
    return `${billing.DocumentNum || ''} - ${this.getClientName()}`;
  }

  hasBuyer(): boolean {
    return !!this.SaleDetail?.Headboard?.ClientCod;
  }

  private async saveBillingForBuyer(): Promise<void> {
    const billing = new SaleBillingEntity();
    billing.SaleCod = this.SaleCod;
    billing.DocumentTypeRequest = '03';
    const response = await this.saleService.saveBilling(billing);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudieron guardar los datos de facturacion.');
      return;
    }
    this.SaleDetail.SaleBilling = response.Data;
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
