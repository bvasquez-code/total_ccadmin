import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { ProductSearchDto } from '../../../product/model/dto/ProductSearchDto';
import { ProductSearchEntity } from '../../../product/model/entity/ProductSearchEntity';
import { ProductService } from '../../../product/service/product.service';
import { ProductSearchService } from '../../../product/service/productsearch.service';
import { ProductUnitHelper } from '../../../shared/helper/ProductUnitHelper';
import { AlertService } from '../../../shared/service/AlertService';
import {
  StockMovementDetail, StockMovementHead, StockMovementKind,
  StockMovementMode, StockMovementRegister
} from '../../model/StockMovementModels';
import { StockMovementService } from '../../service/stock-movement.service';

@Component({
  selector: 'app-stock-movement-form',
  template: `
    <div role="main" class="main-content">
      <div class="page-content container container-plus">
        <div class="card-header">
          <h3 class="card-title text-125 text-primary-d2">
            <i [ngClass]="iconClass" class="text-dark-l3 mr-1"></i>
            {{ title }}
          </h3>
          <div *ngIf="code" class="text-grey-d1 text-90 mt-1">
            Documento {{ code }}
            <span [ngClass]="statusBadge(register.Head.ProcessStatus)" class="ml-1">
              {{ statusName(register.Head.ProcessStatus) }}
            </span>
          </div>
        </div>

        <div class="card dcard">
          <div class="card-body">
            <app-stock-movement-header-form
              [head]="register.Head" [kind]="kind" [reasonList]="reasonList"
              [readonly]="readonly || mode==='resolve'">
            </app-stock-movement-header-form>

            <hr>

            <ng-container *ngIf="mode==='resolve'; else originalEditor">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <h5 class="text-primary mb-0">Productos pendientes de resolucion</h5>
                <span class="text-muted text-90">La resolucion puede ser total o parcial</span>
              </div>
              <div class="alert alert-info py-2">
                Marque las filas que desea procesar e indique cantidad, resolucion y motivo.
              </div>
              <app-stock-resolution-editor
                [detailList]="register.DetailList"
                [releaseReasonList]="releaseReasonList"
                [withdrawReasonList]="withdrawReasonList">
              </app-stock-resolution-editor>
            </ng-container>

            <ng-template #originalEditor>
              <div class="row">
                <div class="col-md-5" *ngIf="!readonly">
                  <h5 class="text-primary">Productos</h5>
                  <div class="d-flex align-items-center mb-2">
                    <input type="text" class="form-control mr-2" placeholder="Buscar producto"
                           [(ngModel)]="productQuery" (keyup.enter)="searchProducts()">
                    <button class="btn btn-primary" (click)="searchProducts()" [disabled]="saving">
                      Buscar
                    </button>
                  </div>

                  <div class="table-responsive">
                    <table class="table table-bordered">
                      <thead>
                        <tr>
                          <th>Codigo</th>
                          <th>Producto</th>
                          <th class="text-center">Stock</th>
                          <th class="text-center">Seleccionar</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr *ngFor="let product of productList">
                          <td>{{ product.ProductCod }}</td>
                          <td>{{ product.ProductName }}</td>
                          <td class="text-center">
                            {{ visibleProductStock(product) }} {{ product.ProductUnitName }}
                          </td>
                          <td class="text-center">
                            <button type="button" class="btn btn-sm btn-outline-primary"
                                    data-toggle="modal" data-target="#modalExceptionalStockProduct"
                                    (click)="selectProduct(product)" [disabled]="saving">
                              Elegir
                            </button>
                          </td>
                        </tr>
                        <tr *ngIf="productList.length===0">
                          <td colspan="4" class="text-center text-muted">Busque un producto para agregarlo</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <div [ngClass]="readonly ? 'col-md-12' : 'col-md-7'">
                  <div class="d-flex justify-content-between align-items-center mb-2">
                    <h5 class="text-primary mb-0">Detalle</h5>
                    <span class="text-muted text-90">{{ register.DetailList.length }} producto(s)</span>
                  </div>
                  <app-stock-movement-detail-editor
                    [detailList]="register.DetailList"
                    [warehouseList]="warehouseList"
                    [unavailableReasonList]="unavailableReasonList"
                    [useUnavailableReason]="register.Head.MovementMode==='N'"
                    [readonly]="readonly"
                    (remove)="removeDetail($event)">
                  </app-stock-movement-detail-editor>
                </div>
              </div>
            </ng-template>

            <hr>

            <div class="mt-3">
              <button *ngIf="!readonly && mode!=='resolve'" class="btn btn-primary" (click)="save(false)" [disabled]="saving">
                <i class="fa fa-save mr-1"></i>
                {{ saving ? 'Guardando...' : 'Guardar pendiente' }}
              </button>
              <button *ngIf="!readonly" class="btn btn-success ml-2" (click)="save(true)" [disabled]="saving">
                <i class="fa fa-check mr-1"></i>
                {{ mode === 'resolve' ? 'Confirmar resolucion' : 'Guardar y confirmar' }}
              </button>
              <button class="btn btn-secondary ml-2" (click)="back()" [disabled]="saving">
                {{ readonly ? 'Volver' : 'Cancelar' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" id="modalExceptionalStockProduct" tabindex="-1" role="dialog" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title text-primary-d3">Agregar producto</h5>
            <button type="button" class="close" data-dismiss="modal" aria-label="Close" #btnCloseProductModal>
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>Producto</label>
              <input type="text" class="form-control"
                     [value]="selectedProduct.ProductCod ? selectedProduct.ProductCod + ' - ' + selectedProduct.ProductName : ''"
                     disabled>
            </div>
            <div class="form-group">
              <label>Almacen</label>
              <select class="form-control" [(ngModel)]="selectedWarehouse">
                <option value="">Seleccione</option>
                <option *ngFor="let warehouse of warehouseList" [value]="warehouse.WarehouseCod">
                  {{ warehouse.WarehouseName }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>Cantidad</label>
              <div class="input-group">
                <input type="number" min="0" class="form-control" [(ngModel)]="visibleQuantity"
                       (keyup.enter)="addProduct()">
                <div class="input-group-append">
                  <span class="input-group-text">{{ selectedProduct.ProductUnitName || 'NIU' }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
            <button type="button" class="btn btn-primary" (click)="addProduct()" [disabled]="saving">
              Agregar
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class StockMovementFormComponent implements OnInit {
  @ViewChild('btnCloseProductModal') btnCloseProductModal!: ElementRef<HTMLButtonElement>;
  @Input() kind: StockMovementKind = 'entry';
  @Input() mode: StockMovementMode = 'create';
  register: StockMovementRegister = new StockMovementRegister();
  reasonList: any[] = [];
  unavailableReasonList: any[] = [];
  releaseReasonList: any[] = [];
  withdrawReasonList: any[] = [];
  warehouseList: any[] = [];
  productList: ProductSearchEntity[] = [];
  selectedProduct: ProductSearchEntity = new ProductSearchEntity();
  productQuery: string = '';
  selectedWarehouse: string = '';
  visibleQuantity: number = 0;
  code: string = '';
  originCode: string = '';
  saving: boolean = false;

  constructor(
    private service: StockMovementService,
    private productSearchService: ProductSearchService,
    private productService: ProductService,
    private session: DataSesionService,
    private route: ActivatedRoute,
    private router: Router,
    private toastr: ToastrService,
    private alert: AlertService
  ) {}

  ngOnInit(): void {
    this.code = this.route.snapshot.queryParamMap.get('code') || '';
    this.originCode = this.route.snapshot.queryParamMap.get('originCode') || '';
    if (this.mode === 'resolve') this.code = this.originCode;
    void this.load();
  }

  get readonly(): boolean {
    if (this.mode === 'resolve') return false;
    return this.mode === 'view' || (this.register.Head.ProcessStatus !== 'P' && !!this.code);
  }

  get title(): string {
    const noun = this.kind === 'entry' ? 'entrada' : 'retiro';
    if (this.mode === 'resolve') return `Resolver ${noun} de stock no disponible`;
    if (this.mode === 'view') return `Detalle de ${noun} excepcional`;
    return `${this.code ? 'Editar' : 'Crear'} ${noun} excepcional de stock`;
  }

  get iconClass(): string {
    if (this.mode === 'view') return 'fa fa-eye';
    if (this.mode === 'resolve') return 'fa fa-gavel';
    return this.kind === 'entry' ? 'fa fa-plus-circle' : 'fa fa-minus-circle';
  }

  private async load(): Promise<void> {
    const loadCode = this.mode === 'resolve' ? this.originCode : this.code;
    const response = await this.service.findDataForm(this.kind, loadCode);
    if (response.ErrorStatus) { this.toastr.error(response.Message); return; }
    this.reasonList = this.catalogByGroup(
      this.additional(response, 'reasonList'),
      this.kind === 'entry' ? 8 : 9
    );
    this.unavailableReasonList = this.catalogByGroup(
      this.additional(response, 'unavailableReasonList'),
      10
    );
    this.releaseReasonList = this.catalogByGroup(
      this.additional(response, 'releaseReasonList'),
      11
    );
    this.withdrawReasonList = this.catalogByGroup(
      this.additional(response, 'withdrawReasonList'),
      12
    );
    this.warehouseList = this.additional(response, 'warehouseList');
    const loaded = this.additional(response, 'movement');

    if (loaded) {
      if (this.mode === 'resolve') this.prepareResolution(loaded);
      else this.register = this.hydrate(loaded);
    } else {
      this.register.Head.ProcessType = 'O';
      this.register.Head.MovementMode = 'D';
      this.register.Head.ProcessStatus = 'P';
      this.register.Head.StoreCod = this.session.getSessionStorageDto().StoreCod;
    }
  }

  private prepareResolution(source: any): void {
    const original = this.hydrate(source);
    if (original.Head.ProcessStatus !== 'C' || original.Head.MovementMode !== 'N') {
      this.toastr.error('El documento no tiene stock no disponible pendiente de resolucion');
      return;
    }
    const detailList = original.DetailList.filter(d => d.NumUnitPending > 0).map(sourceDetail => {
      const detail = Object.assign(new StockMovementDetail(), sourceDetail);
      detail.Selected = false;
      detail.VisibleQuantity = 0;
      detail.ResolutionType = null;
      detail.ResolutionReasonCode = null;
      detail.Observation = '';
      detail.NextReviewDate = null;
      return detail;
    });
    this.register = Object.assign(new StockMovementRegister(), {
      Head: original.Head,
      DetailList: detailList
    });
  }

  async searchProducts(): Promise<void> {
    const request = new ProductSearchDto();
    request.Query = this.productQuery;
    request.Page = 1;
    request.StoreCod = this.session.getSessionStorageDto().StoreCod;
    request.StockMin = this.kind === 'exit' ? 1 : 0;
    const response = await this.productSearchService.query(request);
    if (response.ErrorStatus) this.toastr.error(response.Message);
    else this.productList = response.Data?.resultSearch || [];
  }

  selectProduct(product: ProductSearchEntity): void {
    this.selectedProduct = product;
    this.selectedWarehouse = this.warehouseList.length === 1
      ? this.warehouseList[0].WarehouseCod
      : '';
    this.visibleQuantity = 0;
  }

  async addProduct(): Promise<void> {
    try {
      const product = this.selectedProduct;
      if (!product) throw new Error('Seleccione un producto');
      if (!product.ProductCod) throw new Error('Seleccione un producto');
      if (!this.selectedWarehouse) throw new Error('Seleccione un almacen');
      if (!(this.visibleQuantity > 0)) throw new Error('Ingrese una cantidad mayor a cero');
      const infoResponse = await this.productService.findDetailById(
        product.ProductCod, this.session.getSessionStorageDto().StoreCod
      );
      if (infoResponse.ErrorStatus) throw new Error(infoResponse.Message);
      const info = infoResponse.Data;
      const factor = Number(info.Config?.ProductUnitFactor || product.ProductUnitFactor || 1);
      const internalQuantity = ProductUnitHelper.toInternalQuantity(this.visibleQuantity, factor);
      if (!Number.isInteger(internalQuantity)) throw new Error('La cantidad no representa unidades internas completas');
      const detail = new StockMovementDetail();
      detail.ProductCod = product.ProductCod;
      detail.ProductName = product.ProductName;
      detail.Variant = info.VariantList?.[0]?.Variant || '0000';
      detail.WarehouseCod = this.selectedWarehouse;
      detail.ProductUnitName = info.Config?.ProductUnitName || product.ProductUnitName || 'NIU';
      detail.ProductUnitFactor = factor;
      detail.NumUnit = internalQuantity;
      if (this.register.DetailList.some(item =>
        item.ProductCod === detail.ProductCod && item.Variant === detail.Variant
        && item.WarehouseCod === detail.WarehouseCod && (item.LotNumber || '') === '')) {
        throw new Error('El producto ya fue agregado para el mismo almacen');
      }
      this.register.DetailList.push(detail);
      this.selectedProduct = new ProductSearchEntity();
      this.selectedWarehouse = '';
      this.visibleQuantity = 0;
      this.btnCloseProductModal?.nativeElement.click();
    } catch (error: any) {
      this.toastr.error(error.message);
    }
  }

  removeDetail(index: number): void { this.register.DetailList.splice(index, 1); }

  async save(confirm: boolean): Promise<void> {
    if (this.saving) return;
    try {
      this.saving = true;
      if (this.mode === 'resolve') {
        await this.resolve();
        return;
      }
      const request = this.buildRequest();
      if (confirm) {
        const answer = await this.alert.waring(
          'La confirmacion movera el stock y el documento ya no podra editarse.',
          'Confirmar movimiento'
        );
        if (!answer.isConfirmed) return;
      }
      const saveResponse = await this.service.save(this.kind, request);
      if (saveResponse.ErrorStatus) throw new Error(saveResponse.Message);
      const saved = this.hydrate(saveResponse.Data);
      const savedCode = this.documentCode(saved.Head);
      if (confirm) {
        const confirmResponse = await this.service.confirm(this.kind, savedCode);
        if (confirmResponse.ErrorStatus) throw new Error(confirmResponse.Message);
        this.toastr.success('Stock actualizado y movimiento confirmado');
        this.back();
      } else {
        this.toastr.success('Movimiento guardado como pendiente');
        this.register = saved;
        this.code = savedCode;
        await this.router.navigate([], { relativeTo: this.route, queryParams: { code: savedCode }, replaceUrl: true });
      }
    } catch (error: any) {
      this.toastr.error(error.message || 'No se pudo guardar el movimiento');
    } finally {
      this.saving = false;
    }
  }

  private buildRequest(): StockMovementRegister {
    const request = this.hydrate(this.register);
    if (!request.DetailList.length) throw new Error('Debe agregar al menos un producto');
    if (!request.Head.ReasonCode) throw new Error('Seleccione el motivo');
    if (request.Head.MovementMode === 'N' && request.DetailList.some(item => !item.UnavailableReasonCode)) {
      throw new Error('Seleccione el motivo de no disponible en todos los productos');
    }
    return request;
  }

  private async resolve(): Promise<void> {
    const selected = this.register.DetailList.filter(item => item.Selected);
    if (!selected.length) throw new Error('Seleccione al menos una fila para resolver');
    const detailList = selected.map(item => {
      const quantity = ProductUnitHelper.toInternalQuantity(
        item.VisibleQuantity, Number(item.ProductUnitFactor || 1)
      );
      if (!Number.isInteger(quantity) || quantity <= 0 || quantity > item.NumUnitPending) {
        throw new Error(`Cantidad invalida para ${item.ProductCod}`);
      }
      if (!item.ResolutionType) throw new Error(`Seleccione la resolucion para ${item.ProductCod}`);
      return {
        ItemNumber: item.ItemNumber,
        NumUnit: quantity,
        ResolutionVersion: Number(item.ResolutionVersion || 0),
        ResolutionType: item.ResolutionType,
        ResolutionReasonCode: item.ResolutionReasonCode,
        Observation: item.Observation,
        NextReviewDate: item.NextReviewDate
      };
    });
    const answer = await this.alert.waring(
      'La resolucion actualizara el stock y las cantidades del documento original.',
      'Confirmar resolucion'
    );
    if (!answer.isConfirmed) return;
    const response = await this.service.resolve(this.kind, {
      Code: this.originCode,
      DetailList: detailList
    });
    if (response.ErrorStatus) throw new Error(response.Message);
    this.toastr.success('Resolucion aplicada sobre el documento original');
    this.back();
  }

  back(): void {
    void this.router.navigate([`/enterprise/inventory/pages/list${this.kind === 'entry' ? 'stockentry' : 'stockexit'}`]);
  }

  statusName(status: string): string {
    return ({P: 'Pendiente', C: 'Confirmado', R: 'Rechazado', X: 'Anulado'} as any)[status] || status;
  }

  statusBadge(status: string): string {
    return ({
      P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25',
      C: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
      R: 'badge badge-sm bgc-dark text-white pb-1 px-25',
      X: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
    } as any)[status] || 'badge badge-sm bgc-secondary text-white';
  }

  visibleProductStock(product: ProductSearchEntity): number {
    return ProductUnitHelper.toVisibleQuantity(
      this.kind === 'exit' ? product.NumPhysicalStock : product.NumTotalStock,
      product.ProductUnitFactor
    );
  }

  private documentCode(head: StockMovementHead): string {
    return this.kind === 'entry' ? head.StockEntryCod : head.StockExitCod;
  }

  private hydrate(data: any): StockMovementRegister {
    const result = new StockMovementRegister();
    result.Head = Object.assign(new StockMovementHead(), data?.Head || {});
    result.DetailList = (data?.DetailList || []).map((item: any) => Object.assign(new StockMovementDetail(), item));
    return result;
  }

  private additional(response: any, name: string): any {
    return response.DataAdditional?.find((item: any) => item.Name === name)?.Data;
  }

  private catalogByGroup(catalog: any, expectedGroupId: number): any[] {
    if (!Array.isArray(catalog)) return [];
    return catalog.filter(item => Number(item?.GroupId) === expectedGroupId);
  }
}
