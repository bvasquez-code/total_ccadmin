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
import { SaleService } from '../../service/sale.service';
import { TicketSunatService } from '../../service/TicketSunatService';

@Component({
  selector: 'app-viewsale',
  templateUrl: './viewsale.component.html'
})
export class ViewsaleComponent implements OnInit {

  SaleCod: string = '';
  AutoPrint: boolean = false;
  SaleDetail: SaleDetailDto = new SaleDetailDto();
  PaymentMethodList: PaymentMethodEntity[] = [];
  loading: boolean = false;

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
        await this.print();
      }
    } finally {
      this.loading = false;
    }
  }

  async print(): Promise<void> {
    const response: ResponseWsDto = await this.saleService.findDataPrint(this.SaleCod);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudo imprimir la venta.');
      return;
    }
    await this.ticketSunatService.printSalesInvoice(response);
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
