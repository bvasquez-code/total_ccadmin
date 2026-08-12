import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { CreditNoteDetailDto } from '../../model/dto/CreditNoteDetailDto';
import { SaleDetailDto } from '../../model/dto/SaleDetailDto';
import { SaleDetEntity } from '../../model/entity/SaleDetEntity';
import { SaleDetTaxEntity } from '../../model/entity/SaleDetTaxEntity';
import { SaleDocumentEntity } from '../../model/entity/SaleDocumentEntity';
import { SaleDeliveryEntity } from '../../model/entity/SaleDeliveryEntity';
import { SaleService } from '../../service/sale.service';
import { TicketSunatService } from '../../service/TicketSunatService';

@Component({
  selector: 'app-viewsale',
  templateUrl: './viewsale.component.html'
})
export class ViewsaleComponent implements OnInit {

  SaleCod: string = '';
  AutoPrint: boolean = false;
  DocumentCod: string = '';
  SaleDetail: SaleDetailDto = new SaleDetailDto();
  PaymentMethodList: PaymentMethodEntity[] = [];
  loading: boolean = false;
  ReturnUrl: string = '/enterprise/sale/pages/listsale';

  constructor(
    private route: ActivatedRoute,
    private saleService: SaleService,
    private ticketSunatService: TicketSunatService,
    private toastrService: ToastrService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(async params => {
      this.SaleCod = params.get('SaleCod') || '';
      this.AutoPrint = params.get('AutoPrint') === 'Y';
      this.DocumentCod = params.get('DocumentCod') || '';
      if (params.get('ReturnUrl') === '/enterprise/sale/pages/listsaleweb') {
        this.ReturnUrl = '/enterprise/sale/pages/listsaleweb';
      }
      if (!this.SaleCod) {
        this.toastrService.error('Debe indicar la venta que desea visualizar.');
        return;
      }
      await this.findDataForm(this.SaleCod);
    });
  }

  async findDataForm(saleCod: string): Promise<void> {
    this.loading = true;
    try {
      const response: ResponseWsDto = await this.saleService.findDataForm(saleCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudo cargar la venta.');
        return;
      }

      this.SaleDetail = response.DataAdditional.find(item => item.Name === 'SaleDetail')?.Data
        ?? new SaleDetailDto();
      this.PaymentMethodList = response.DataAdditional.find(item => item.Name === 'PaymentMethodList')?.Data
        ?? [];

      if (this.AutoPrint) {
        this.AutoPrint = false;
        await this.print(this.DocumentCod);
      }
    } finally {
      this.loading = false;
    }
  }

  async print(documentCod: string = ''): Promise<void> {
    const selectedDocumentCod = documentCod || this.DocumentCod || this.SaleDetail?.SaleDocument?.DocumentCod || '';
    const response: ResponseWsDto = await this.saleService.findDataPrint(this.SaleCod, selectedDocumentCod);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudo imprimir la venta.');
      return;
    }
    await this.ticketSunatService.printSaleDocument(response);
  }

  getDocumentList(): SaleDocumentEntity[] {
    const documentList = this.SaleDetail?.SaleDocumentList ?? [];
    if (documentList.length > 0) return documentList;
    return this.SaleDetail?.SaleDocument?.DocumentCod ? [this.SaleDetail.SaleDocument] : [];
  }

  getDocumentTypeName(document: SaleDocumentEntity): string {
    if (document.DocumentType === '99') return 'Proforma';
    if (document.DocumentType === '01') return 'Factura';
    if (document.DocumentType === '03') return 'Boleta';
    return document.DocumentType || 'Documento';
  }

  getDocumentClientName(document: SaleDocumentEntity): string {
    if (document.DocumentRole === 'F' && this.SaleDetail?.SaleBilling?.LegalName) {
      return this.SaleDetail.SaleBilling.LegalName;
    }
    const person = document?.Client?.Person;
    if (!person) return this.getClientName();
    const naturalName = `${person.Names || ''} ${person.LastNames || ''}`.trim();
    return person.BusinessName || person.CommercialName || naturalName || 'Cliente no identificado';
  }

  canIssueFiscalDocument(): boolean {
    return this.SaleDetail?.Headboard?.SaleStatus === 'C'
      && this.SaleDetail?.Headboard?.HasFiscalDocument !== 'S'
      && this.getDocumentList().some(document => document.DocumentType === '99');
  }

  hasClient(): boolean {
    return !!this.SaleDetail?.Headboard?.Client?.Person;
  }

  getClientName(): string {
    const person = this.SaleDetail?.Headboard?.Client?.Person;
    if (!person) return 'Cliente no identificado';

    const naturalName = `${person.Names || ''} ${person.LastNames || ''}`.trim();
    return person.BusinessName || person.CommercialName || naturalName || 'Cliente no identificado';
  }

  hasCreditNote(): boolean {
    return !!this.getCreditNote()?.Headboard?.CreditNoteCod;
  }

  getCreditNote(): CreditNoteDetailDto | null {
    return this.SaleDetail?.CreditNoteDetail || null;
  }

  getCreditNotePurpose(): string {
    return this.getCreditNote()?.Headboard?.IsProductExchange === 'S'
      ? 'Cambio de producto'
      : 'Devolución';
  }

  getCreditNoteStatus(): string {
    const status = this.getCreditNote()?.Headboard?.CreditNoteStatus;
    if (status === 'C') return 'Confirmada';
    if (status === 'P') return 'Pendiente';
    if (status === 'X') return 'Anulada';
    return status || '-';
  }

  hasBilling(): boolean {
    const billing = this.SaleDetail?.SaleBilling;
    return !!billing && !!(billing.DocumentTypeRequest || billing.DocumentNum || billing.LegalName);
  }

  getBillingDocumentName(): string {
    return this.SaleDetail?.SaleBilling?.DocumentTypeRequest === '01' ? 'Factura' : 'Boleta';
  }

  getDelivery(): SaleDeliveryEntity | null {
    const delivery = this.SaleDetail?.SaleDelivery;
    return delivery?.SaleCod ? delivery : null;
  }

  getDeliveryTypeName(): string {
    const deliveryTypeCod = this.getDelivery()?.DeliveryTypeCod;
    if (deliveryTypeCod === 'DELIVERY') return 'Delivery cercano';
    if (deliveryTypeCod === 'STORE_PICKUP') return 'Recojo en tienda';
    if (deliveryTypeCod === 'SCHEDULED_DELIVERY') return 'Entrega programada';
    return deliveryTypeCod || '-';
  }

  getDeliveryTypeBadgeClass(): string {
    const deliveryTypeCod = this.getDelivery()?.DeliveryTypeCod;
    if (deliveryTypeCod === 'DELIVERY') return 'bgc-purple-d1 text-white';
    if (deliveryTypeCod === 'STORE_PICKUP') return 'bgc-pink-d1 text-white';
    if (deliveryTypeCod === 'SCHEDULED_DELIVERY') return 'bgc-brown-d1 text-white';
    return 'bgc-secondary text-white';
  }

  getDeliveryStatusName(): string {
    const deliveryStatus = this.getDelivery()?.DeliveryStatus;
    if (deliveryStatus === 'P') return 'Pendiente';
    if (deliveryStatus === 'S') return 'Programada';
    if (deliveryStatus === 'R') return 'En preparaci\u00f3n';
    if (deliveryStatus === 'L') return 'Lista para recojo';
    if (deliveryStatus === 'D') return 'Despachada';
    if (deliveryStatus === 'E') return 'Entregada';
    if (deliveryStatus === 'X') return 'Cancelada';
    if (deliveryStatus === 'F') return 'Entrega fallida';
    return deliveryStatus || '-';
  }

  getDeliveryStatusBadgeClass(): string {
    const deliveryStatus = this.getDelivery()?.DeliveryStatus;
    if (deliveryStatus === 'P') return 'bgc-warning-d1 text-white';
    if (deliveryStatus === 'S' || deliveryStatus === 'D') return 'bgc-info-d1 text-white';
    if (deliveryStatus === 'R') return 'bgc-primary-d1 text-white';
    if (deliveryStatus === 'L' || deliveryStatus === 'E') return 'bgc-success-d1 text-white';
    if (deliveryStatus === 'F') return 'bgc-danger-d1 text-white';
    return 'bgc-secondary text-white';
  }

  getDeliveryDocumentTypeName(): string {
    const documentType = this.getDelivery()?.DocumentType;
    if (documentType === '01') return 'DNI';
    if (documentType === '04') return 'Carn\u00e9 de extranjer\u00eda';
    if (documentType === '06') return 'RUC';
    if (documentType === '07') return 'Pasaporte';
    return documentType || 'Documento';
  }

  hasDeliveryLocation(): boolean {
    const delivery = this.getDelivery();
    return !!delivery && !!(
      delivery.Address
      || delivery.GeocodedAddress
      || delivery.Reference
      || delivery.CountryName
      || delivery.StateName
      || delivery.CityName
      || delivery.UbigeoCod
      || typeof delivery.Latitude === 'number'
      || typeof delivery.Longitude === 'number'
    );
  }

  hasDeliverySchedule(): boolean {
    const delivery = this.getDelivery();
    return !!delivery && !!(delivery.ScheduledFrom || delivery.ScheduledTo);
  }

  hasDeliveryLogistics(): boolean {
    const delivery = this.getDelivery();
    return !!delivery && !!(
      delivery.ShippingProviderCod
      || delivery.TrackingNumber
      || delivery.AgencyName
      || delivery.AgencyAddress
    );
  }

  hasDeliveryDates(): boolean {
    const delivery = this.getDelivery();
    return !!delivery && !!(delivery.ReadyDate || delivery.DispatchDate || delivery.DeliveredDate);
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
    const affectation = tax.TaxAffectationCod ? `/${tax.TaxAffectationCod}` : '';
    const rate = Number(tax.TaxRateValue || 0);
    const fixed = Number(tax.FixedUnitAmount || 0);
    if (tax.TaxCalculationType === 'P' && rate > 0) return `${name}${affectation} ${rate}%`;
    if (tax.TaxCalculationType === 'F' && fixed > 0) return `${name} ${fixed}`;
    return `${name}${affectation}`;
  }
}
