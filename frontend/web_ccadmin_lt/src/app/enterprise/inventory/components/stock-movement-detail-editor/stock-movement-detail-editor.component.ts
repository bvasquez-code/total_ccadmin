import { Component, EventEmitter, Input, Output } from '@angular/core';
import { StockMovementDetail } from '../../model/StockMovementModels';

@Component({
  selector: 'app-stock-movement-detail-editor',
  template: `
    <div class="table-responsive">
      <table class="table table-bordered">
        <thead>
          <tr>
            <th>Producto</th>
            <th>Almacen</th>
            <th class="text-center">Cantidad</th>
            <th *ngIf="useUnavailableReason">Motivo no disponible</th>
            <th>Resultado</th>
            <th *ngIf="!readonly"></th>
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
            <td [attr.colspan]="useUnavailableReason ? 6 : 5" class="text-center text-muted">
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
  @Output() remove = new EventEmitter<number>();

  get unavailableReasons(): any[] {
    return this.unavailableReasonList.filter(reason => Number(reason?.GroupId) === 10);
  }

  visible(detail: StockMovementDetail): number {
    return Number(detail.NumUnit || 0) / Number(detail.ProductUnitFactor || 1);
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
}
