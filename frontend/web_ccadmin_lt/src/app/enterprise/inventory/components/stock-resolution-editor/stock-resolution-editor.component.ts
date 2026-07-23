import { Component, Input } from '@angular/core';
import { ProductUnitHelper } from '../../../shared/helper/ProductUnitHelper';
import { StockMovementDetail } from '../../model/StockMovementModels';

@Component({
  selector: 'app-stock-resolution-editor',
  template: `
    <div class="table-responsive">
      <table class="table table-bordered">
        <thead>
          <tr>
            <th class="text-center">Resolver</th>
            <th>Producto</th>
            <th class="text-center">Pendiente</th>
            <th>Cantidad</th>
            <th>Resolucion</th>
            <th>Motivo</th>
            <th>Observacion / revision</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let detail of detailList">
            <td class="text-center align-middle">
              <input type="checkbox" [(ngModel)]="detail.Selected">
            </td>
            <td>
              {{detail.ProductCod}} - {{detail.ProductName || 'Producto'}}
              <small class="d-block text-muted">Variante: {{detail.Variant}}</small>
            </td>
            <td class="text-center">{{ format(detail.NumUnitPending, detail) }}</td>
            <td><input type="number" min="0" class="form-control" [(ngModel)]="detail.VisibleQuantity" [disabled]="!detail.Selected"></td>
            <td>
              <select class="form-control" [(ngModel)]="detail.ResolutionType"
                      (ngModelChange)="resolutionTypeChanged(detail)" [disabled]="!detail.Selected">
                <option [ngValue]="null">Seleccione</option>
                <option value="L">Liberar a disponible</option>
                <option value="B">Baja definitiva</option>
                <option value="D">Destruir</option>
                <option value="M">Mantener no disponible</option>
              </select>
            </td>
            <td>
              <select class="form-control" [(ngModel)]="detail.ResolutionReasonCode"
                      [disabled]="!detail.Selected || detail.ResolutionType==='M'">
                <option [ngValue]="null">Seleccione</option>
                <option *ngFor="let reason of reasons(detail.ResolutionType)" [value]="reason.ConfigCod">
                  {{reason.ConfigDesc || reason.ConfigName || reason.ConfigCod}}
                </option>
              </select>
            </td>
            <td>
              <input class="form-control mb-1" placeholder="Observacion" [(ngModel)]="detail.Observation" [disabled]="!detail.Selected">
              <input *ngIf="detail.ResolutionType==='M'" type="date" class="form-control"
                     [(ngModel)]="detail.NextReviewDate" [disabled]="!detail.Selected">
            </td>
          </tr>
          <tr *ngIf="detailList.length===0">
            <td colspan="7" class="text-center text-muted">No existen cantidades pendientes de resolucion</td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class StockResolutionEditorComponent {
  @Input() detailList: StockMovementDetail[] = [];
  @Input() releaseReasonList: any[] = [];
  @Input() withdrawReasonList: any[] = [];

  reasons(type: string | null): any[] {
    if (type === 'L') {
      return this.releaseReasonList.filter(reason => Number(reason?.GroupId) === 11);
    }
    if (type === 'B' || type === 'D') {
      return this.withdrawReasonList.filter(reason => Number(reason?.GroupId) === 12);
    }
    return [];
  }

  resolutionTypeChanged(detail: StockMovementDetail): void {
    detail.ResolutionReasonCode = null;
    if (detail.ResolutionType !== 'M') detail.NextReviewDate = null;
  }

  format(quantity: number, detail: StockMovementDetail): string {
    return ProductUnitHelper.formatVisibleQuantity(quantity, detail.ProductUnitFactor, detail.ProductUnitName);
  }
}
