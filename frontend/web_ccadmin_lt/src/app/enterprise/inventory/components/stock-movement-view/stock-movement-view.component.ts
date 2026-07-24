import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProductUnitHelper } from '../../../shared/helper/ProductUnitHelper';
import {
  StockMovementDetail,
  StockMovementHead,
  StockMovementKind,
  StockMovementRegister
} from '../../model/StockMovementModels';
import { StockMovementService } from '../../service/stock-movement.service';

@Component({
  selector: 'app-stock-movement-view',
  templateUrl: './stock-movement-view.component.html'
})
export class StockMovementViewComponent implements OnInit {
  @Input() kind: StockMovementKind = 'entry';

  register: StockMovementRegister = new StockMovementRegister();
  reasonList: any[] = [];
  unavailableReasonList: any[] = [];
  releaseReasonList: any[] = [];
  withdrawReasonList: any[] = [];
  warehouseList: any[] = [];
  code: string = '';
  loading: boolean = true;

  constructor(
    private service: StockMovementService,
    private route: ActivatedRoute,
    private router: Router,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.code = this.route.snapshot.queryParamMap.get('code') || '';
    void this.load();
  }

  get noun(): string {
    return this.kind === 'entry' ? 'entrada' : 'retiro';
  }

  get sectionTitle(): string {
    return this.kind === 'entry' ? 'Entradas de stock' : 'Retiros de stock';
  }

  get head(): StockMovementHead {
    return this.register.Head;
  }

  private async load(): Promise<void> {
    if (!this.code) {
      this.toastr.error('No se indicó el código del movimiento');
      this.goBack();
      return;
    }
    try {
      const response = await this.service.findDataForm(this.kind, this.code);
      if (response.ErrorStatus) throw new Error(response.Message);

      const movement = this.additional(response, 'movement');
      if (!movement) throw new Error('No se encontró el movimiento de stock');

      this.register = this.hydrate(movement);
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
      this.warehouseList = this.additional(response, 'warehouseList') || [];
    } catch (error: any) {
      this.toastr.error(error.message || 'No se pudo consultar el movimiento');
    } finally {
      this.loading = false;
    }
  }

  movementModeLabel(mode: string | null): string {
    return mode === 'N' ? 'Con paso por no disponible' : 'Directo';
  }

  statusLabel(status: string): string {
    return ({P: 'Pendiente', C: 'Confirmado', R: 'Rechazado', X: 'Anulado'} as any)[status] || status;
  }

  statusClass(status: string): string {
    return ({
      P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25',
      C: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
      R: 'badge badge-sm bgc-dark text-white pb-1 px-25',
      X: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
    } as any)[status] || 'badge badge-sm bgc-secondary-l2 text-dark pb-1 px-25';
  }

  detailStatusLabel(detail: StockMovementDetail): string {
    if (Number(detail.NumUnitPending || 0) > 0) return 'En observación';
    return this.head.MovementMode === 'D' ? 'Aplicado' : 'Resuelto';
  }

  detailStatusClass(detail: StockMovementDetail): string {
    return Number(detail.NumUnitPending || 0) > 0
      ? 'badge badge-sm bgc-orange-d1 text-white pb-1 px-25'
      : 'badge badge-sm bgc-green-d1 text-white pb-1 px-25';
  }

  reasonName(code: string | null, catalog: any[]): string {
    if (!code) return '—';
    const reason = catalog.find(item => item.ConfigCod === code);
    return reason?.ConfigDesc || reason?.ConfigName || reason?.ConfigVal || code;
  }

  warehouseName(code: string): string {
    const warehouse = this.warehouseList.find(item => item.WarehouseCod === code);
    return warehouse?.WarehouseName || code;
  }

  quantity(value: number, detail: StockMovementDetail): string {
    return ProductUnitHelper.formatVisibleQuantity(
      Number(value || 0),
      Number(detail.ProductUnitFactor || 1),
      detail.ProductUnitName
    );
  }

  lotNumber(value: string): string {
    return value && value.trim() ? value : 'SN';
  }

  resolvedOutReason(detail: StockMovementDetail): string {
    const prefix = detail.ResolvedOutType === 'D' ? 'Destrucción' : 'Baja';
    return `${prefix}: ${this.reasonName(detail.ResolvedOutReasonCode, this.withdrawReasonList)}`;
  }

  goBack(): void {
    void this.router.navigate([
      `/enterprise/inventory/pages/list${this.kind === 'entry' ? 'stockentry' : 'stockexit'}`
    ]);
  }

  private hydrate(data: any): StockMovementRegister {
    const result = new StockMovementRegister();
    result.Head = Object.assign(new StockMovementHead(), data?.Head || {});
    result.DetailList = (data?.DetailList || []).map(
      (item: any) => Object.assign(new StockMovementDetail(), item)
    );
    return result;
  }

  private additional(response: any, name: string): any {
    return response.DataAdditional?.find((item: any) => item.Name === name)?.Data;
  }

  private catalogByGroup(catalog: any, groupId: number): any[] {
    if (!Array.isArray(catalog)) return [];
    return catalog.filter(item => Number(item?.GroupId) === groupId);
  }
}
