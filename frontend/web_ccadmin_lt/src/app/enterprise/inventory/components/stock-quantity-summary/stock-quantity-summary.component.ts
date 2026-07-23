import { Component, Input } from '@angular/core';
import { ProductUnitHelper } from '../../../shared/helper/ProductUnitHelper';
import { StockMovementDetail } from '../../model/StockMovementModels';

@Component({
  selector: 'app-stock-quantity-summary',
  template: `
    <div class="text-90">
      <div><span class="text-grey-d1">Solicitado:</span> {{ format(detail.NumUnit) }}</div>
      <div *ngIf="detail.NumUnitPending > 0" class="text-warning">
        <span class="text-grey-d1">Pendiente:</span> {{ format(detail.NumUnitPending) }}
      </div>
      <div *ngIf="detail.NumUnitResolvedIn > 0" class="text-success">
        <span class="text-grey-d1">Entra:</span> {{ format(detail.NumUnitResolvedIn) }}
        <small *ngIf="detail.ResolvedInReasonCode" class="d-block text-muted">
          Motivo: {{ detail.ResolvedInReasonCode }}
        </small>
      </div>
      <div *ngIf="detail.NumUnitResolvedOut > 0" class="text-danger">
        <span class="text-grey-d1">Sale:</span> {{ format(detail.NumUnitResolvedOut) }}
        <small *ngIf="detail.ResolvedOutReasonCode" class="d-block text-muted">
          {{ detail.ResolvedOutType === 'D' ? 'Destruccion' : 'Baja' }}:
          {{ detail.ResolvedOutReasonCode }}
        </small>
      </div>
    </div>
  `
})
export class StockQuantitySummaryComponent {
  @Input() detail: StockMovementDetail = new StockMovementDetail();

  format(quantity: number): string {
    return ProductUnitHelper.formatVisibleQuantity(
      quantity, this.detail.ProductUnitFactor, this.detail.ProductUnitName
    );
  }
}
