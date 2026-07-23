import { Component, EventEmitter, Input, Output } from '@angular/core';
import { StockMovementHead, StockMovementKind } from '../../model/StockMovementModels';

@Component({
  selector: 'app-stock-movement-header-form',
  template: `
    <div class="row">
      <div class="col-md-3 mb-3">
        <label>Modalidad</label>
        <select class="form-control" [(ngModel)]="head.MovementMode" [disabled]="readonly">
          <option value="D">Directo</option>
          <option value="N">Con paso por no disponible</option>
        </select>
      </div>
      <div class="col-md-4 mb-3">
        <label>Motivo de {{ kind === 'entry' ? 'entrada' : 'retiro' }}</label>
        <select class="form-control" [(ngModel)]="head.ReasonCode" [disabled]="readonly">
          <option [ngValue]="null">Seleccione</option>
          <option *ngFor="let reason of reasonList" [value]="reason.ConfigCod">
            {{ reason.ConfigDesc || reason.ConfigName || reason.ConfigVal || reason.ConfigCod }}
          </option>
        </select>
      </div>
      <div class="col-md-5 mb-3">
        <label>Observacion general</label>
        <textarea class="form-control" rows="2" [(ngModel)]="head.Observation" [disabled]="readonly"></textarea>
      </div>
    </div>
  `
})
export class StockMovementHeaderFormComponent {
  @Input() head: StockMovementHead = new StockMovementHead();
  @Input() kind: StockMovementKind = 'entry';
  @Input() reasonList: any[] = [];
  @Input() readonly: boolean = false;
  @Output() headChange = new EventEmitter<StockMovementHead>();

}
