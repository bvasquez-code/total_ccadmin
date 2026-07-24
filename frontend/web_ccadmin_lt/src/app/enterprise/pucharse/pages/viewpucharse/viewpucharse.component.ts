import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { PucharseService } from '../../service/PucharseService';
import { PucharseDetailsDto } from '../../model/dto/PucharseDetailsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { WarehouseEntity } from 'src/app/enterprise/shared/model/entity/WarehouseEntity';
import { PucharsePrintService } from '../../service/PucharsePrintService';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
import { ToastrService } from 'ngx-toastr';
import { PucharseRequestHeadService } from '../../service/PucharseRequestHeadService';
import { PucharseRequestRegisterDto } from '../../model/dto/PucharseRequestRegisterDto';
import { PucharseRequestDetEntity } from '../../model/entity/PucharseRequestDetEntity';

@Component({
  selector: 'app-viewpucharse',
  templateUrl: './viewpucharse.component.html'
})
export class ViewpucharseComponent {

  PucharseCod: string = '';
  PucharseDetails: PucharseDetailsDto = new PucharseDetailsDto();
  Store: StoreEntity = new StoreEntity();
  WarehouseList: WarehouseEntity[] = [];
  isCopying: boolean = false;

  constructor(
    private pucharseService: PucharseService,
    private router: Router,
    private pucharsePrintService: PucharsePrintService,
    private pucharseRequestHeadService: PucharseRequestHeadService,
    private alertService: AlertService,
    private toastrService: ToastrService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.PucharseCod = urlTree.queryParams['PucharseCod'] ?? '';
    this.FindDataForm(this.PucharseCod);
  }

  async FindDataForm(PucharseCod: string): Promise<void> {
    const rpt = await this.pucharseService.FindDataForm(PucharseCod);

    if (!rpt.ErrorStatus) {
      this.PucharseDetails = rpt.DataAdditional.find(e => e.Name === 'PucharseDetails')?.Data;
      this.Store = rpt.DataAdditional.find(e => e.Name === 'Store')?.Data;
      this.WarehouseList = rpt.DataAdditional.find(e => e.Name === 'WarehouseList')?.Data;
    }
  }

  getStatusLabel(status: string): string {
    const map: { [key: string]: string } = {
      F: 'Finalizado',
      P: 'Pendiente'
    };
    return map[status] ?? status;
  }

  getStatusClass(status: string): string {
    const map: { [key: string]: string } = {
      F: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
      P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
    };
    return map[status] ?? 'badge badge-sm bgc-secondary-l2 text-dark pb-1 px-25';
  }

  getKardexLabel(value: string): string {
    return value === 'S' ? 'Recibido' : 'Pendiente';
  }

  getKardexClass(value: string): string {
    return value === 'S'
      ? 'badge badge-sm bgc-green-d1 text-white pb-1 px-25'
      : 'badge badge-sm bgc-orange-d1 text-white pb-1 px-25';
  }

  getLotNumber(value: string): string {
    return value && value.trim() ? value : 'SN';
  }

  get NumTotalPrice(): number {
    return this.PucharseDetails?.Headboard?.NumTotalPrice ?? 0;
  }

  print(): void {
    this.pucharsePrintService.printReception(this.PucharseDetails, this.PucharseDetails.DetailList || [], this.Store, this.WarehouseList);
  }

  async copyPucharse(): Promise<void> {
    if (this.isCopying) return;

    const confirmation = await this.alertService.waring(
      'Se creará una nueva solicitud de compra pendiente con el mismo proveedor, productos, cantidades y precios. El comentario y la operación de referencia quedarán vacíos.',
      '¿Copiar esta compra?'
    );

    if (!confirmation.isConfirmed) return;

    const request = this.buildCopyRequest();
    if (request.DetailList.length === 0) {
      this.toastrService.error('La compra no tiene productos para copiar');
      return;
    }

    this.isCopying = true;

    try {
      const rpt = await this.pucharseRequestHeadService.Save(request);

      if (rpt.ErrorStatus || !rpt.Data?.Headboard?.PucharseReqCod) {
        this.toastrService.error(rpt.Message || 'No se pudo copiar la compra');
        return;
      }

      const pucharseReqCod: string = rpt.Data.Headboard.PucharseReqCod;
      this.toastrService.success('La compra se copió como una nueva solicitud pendiente');
      await this.router.navigate(
        ['/enterprise/pucharse/pages/createpucharse'],
        { queryParams: { PucharseReqCod: pucharseReqCod } }
      );
    } catch {
      this.toastrService.error('Ocurrió un error al copiar la compra');
    } finally {
      this.isCopying = false;
    }
  }

  private buildCopyRequest(): PucharseRequestRegisterDto {
    const request = new PucharseRequestRegisterDto();
    const sourceHead = this.PucharseDetails.Headboard;

    request.Headboard.DealerCod = sourceHead.DealerCod;
    request.Headboard.ExternalCod = '';
    request.Headboard.Commenter = '';
    request.Headboard.PurchaseStatus = 'P';
    request.Headboard.CurrencyCod = sourceHead.CurrencyCod;
    request.Headboard.NumTotalPrice = 0;

    const detailByProduct = new Map<string, PucharseRequestDetEntity>();

    for (const sourceDetail of this.PucharseDetails.DetailList || []) {
      const variant = sourceDetail.Variant || '0000';
      const key = `${sourceDetail.ProductCod}|${variant}`;
      const existing = detailByProduct.get(key);

      if (existing) {
        existing.NumUnit += sourceDetail.NumUnit;
        existing.NumTotalPrice += sourceDetail.NumTotalPrice;
        continue;
      }

      const detail = new PucharseRequestDetEntity();
      detail.ProductCod = sourceDetail.ProductCod;
      detail.Variant = variant;
      detail.NumUnit = sourceDetail.NumUnit;
      detail.NumUnitPrice = sourceDetail.NumUnitPrice;
      detail.NumTotalPrice = sourceDetail.NumTotalPrice;
      detail.ProductUnitName = sourceDetail.ProductUnitName || 'NIU';
      detail.ProductUnitFactor = sourceDetail.ProductUnitFactor > 0
        ? sourceDetail.ProductUnitFactor
        : 1;
      detail.Product = sourceDetail.Product;
      detailByProduct.set(key, detail);
    }

    request.DetailList = Array.from(detailByProduct.values());
    request.Headboard.NumTotalPrice = request.DetailList
      .reduce((total, detail) => total + detail.NumTotalPrice, 0);

    return request;
  }

  goBack(): void {
    this.router.navigate(['/enterprise/pucharse/pages/listpucharse']);
  }
}
