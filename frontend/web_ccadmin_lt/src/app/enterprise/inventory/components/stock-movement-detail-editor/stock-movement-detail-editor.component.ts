import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ProductUnitHelper } from '../../../shared/helper/ProductUnitHelper';
import { StockMovementDetail } from '../../model/StockMovementModels';

@Component({
  selector: 'app-stock-movement-detail-editor',
  template: `
    <div class="table-responsive">
      <table class="table table-bordered">
        <thead>
          <tr>
            <th style="min-width: 190px;">Producto</th>
            <th style="min-width: 115px;">Almacen</th>
            <th class="text-center" style="min-width: 90px;">Cantidad</th>
            <th *ngIf="showLots" style="min-width: 115px;">Lote</th>
            <th *ngIf="showLots" style="min-width: 165px;">Vencimiento</th>
            <th *ngIf="showPrices" class="text-center" style="min-width: 145px;">Precio unitario</th>
            <th *ngIf="showPrices" class="text-center" style="min-width: 145px;">Precio total</th>
            <th *ngIf="useUnavailableReason" style="min-width: 190px;">Motivo no disponible</th>
            <th style="min-width: 145px;">Resultado</th>
            <th *ngIf="!readonly" style="width: 50px;"></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let detail of detailList; let i=index">
            <td>
              {{ detail.ProductCod }} - {{ detail.ProductName || 'Producto' }}
              <small class="d-block text-muted">Variante: {{ detail.Variant }}</small>
            </td>
            <td>{{ warehouseName(detail.WarehouseCod) }}</td>
            <td class="text-center">{{ visible(detail) }} {{ detail.ProductUnitName }}</td>
            <td *ngIf="showLots">
              <input type="text" maxlength="32" class="form-control form-control-sm"
                     [(ngModel)]="detail.LotNumber" [disabled]="readonly">
            </td>
            <td *ngIf="showLots">
              <input type="date" class="form-control form-control-sm"
                     [(ngModel)]="detail.ExpirationDate" [disabled]="readonly">
            </td>
            <td *ngIf="showPrices" class="text-center">
              <div class="input-group input-group-sm">
                <div class="input-group-prepend"><span class="input-group-text">S/</span></div>
                <input type="number" min="0" step="0.01" class="form-control text-right"
                       [ngModel]="visibleUnitPrice(detail)"
                       (ngModelChange)="updateFromUnitPrice(detail, $event)"
                       [disabled]="readonly">
              </div>
            </td>
            <td *ngIf="showPrices" class="text-center">
              <div class="input-group input-group-sm">
                <div class="input-group-prepend"><span class="input-group-text">S/</span></div>
                <input type="number" min="0" step="0.01" class="form-control text-right"
                       [ngModel]="detail.NumTotalPrice"
                       (ngModelChange)="updateFromTotalPrice(detail, $event)"
                       [disabled]="readonly">
              </div>
            </td>
            <td *ngIf="useUnavailableReason">
              <select class="form-control" [(ngModel)]="detail.UnavailableReasonCode" [disabled]="readonly">
                <option [ngValue]="null">Seleccione</option>
                <option *ngFor="let reason of unavailableReasons" [value]="reason.ConfigCod">
                  {{ reason.ConfigDesc || reason.ConfigName || reason.ConfigVal || reason.ConfigCod }}
                </option>
              </select>
            </td>
            <td>
              <app-stock-quantity-summary *ngIf="hasResult(detail); else pendingResult"
                                          [detail]="detail">
              </app-stock-quantity-summary>
              <ng-template #pendingResult><span class="text-muted">Pendiente de confirmacion</span></ng-template>
            </td>
            <td *ngIf="!readonly">
              <button type="button" class="btn btn-sm btn-danger" title="Quitar producto" (click)="remove.emit(i)">
                <i class="fa fa-trash"></i>
              </button>
            </td>
          </tr>
          <tr *ngIf="detailList.length===0">
            <td [attr.colspan]="emptyColspan" class="text-center text-muted">
              Sin productos seleccionados
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class StockMovementDetailEditorComponent {
  @Input() detailList: StockMovementDetail[] = [];
  @Input() warehouseList: any[] = [];
  @Input() unavailableReasonList: any[] = [];
  @Input() useUnavailableReason: boolean = false;
  @Input() readonly: boolean = false;
  @Input() showPrices: boolean = false;
  @Input() showLots: boolean = false;
  @Output() remove = new EventEmitter<number>();
  @Output() pricesChange = new EventEmitter<void>();

  get emptyColspan(): number {
    return 4 + (this.readonly ? 0 : 1)
      + (this.useUnavailableReason ? 1 : 0)
      + (this.showPrices ? 2 : 0)
      + (this.showLots ? 2 : 0);
  }

  get unavailableReasons(): any[] {
    return this.unavailableReasonList.filter(reason => Number(reason?.GroupId) === 10);
  }

  visible(detail: StockMovementDetail): number {
    return Number(detail.NumUnit || 0) / Number(detail.ProductUnitFactor || 1);
  }

  visibleUnitPrice(detail: StockMovementDetail): number {
    return this.roundMoney(ProductUnitHelper.toVisibleUnitPrice(
      Number(detail.NumUnitPrice || 0),
      Number(detail.ProductUnitFactor || 1)
    ));
  }

  updateFromUnitPrice(detail: StockMovementDetail, value: number | string | null): void {
    const visibleUnitPrice = this.nonNegativeMoney(value);
    detail.NumUnitPrice = ProductUnitHelper.toInternalUnitPrice(
      visibleUnitPrice,
      Number(detail.ProductUnitFactor || 1)
    );
    detail.NumTotalPrice = this.roundMoney(detail.NumUnitPrice * Number(detail.NumUnit || 0));
    this.pricesChange.emit();
  }

  updateFromTotalPrice(detail: StockMovementDetail, value: number | string | null): void {
    detail.NumTotalPrice = this.nonNegativeMoney(value);
    detail.NumUnitPrice = Number(detail.NumUnit || 0) > 0
      ? detail.NumTotalPrice / Number(detail.NumUnit)
      : 0;
    this.pricesChange.emit();
  }

  hasResult(detail: StockMovementDetail): boolean {
    return Number(detail.NumUnitPending || 0) > 0
      || Number(detail.NumUnitResolvedIn || 0) > 0
      || Number(detail.NumUnitResolvedOut || 0) > 0;
  }

  warehouseName(code: string): string {
    const item = this.warehouseList.find(w => w.WarehouseCod === code);
    return item?.WarehouseName || code;
  }

  private nonNegativeMoney(value: number | string | null): number {
    const amount = Number(value || 0);
    return this.roundMoney(Number.isFinite(amount) && amount > 0 ? amount : 0);
  }

  private roundMoney(value: number): number {
    return Number(Number(value || 0).toFixed(2));
  }
}
