import { Component, Input, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from '../../../shared/interface/ActionModalConfirmService';
import { ActionTableService } from '../../../shared/interface/ActionTableService';
import { DataTablaGeneticDto } from '../../../shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { StockMovementKind } from '../../model/StockMovementModels';
import { StockMovementService } from '../../service/stock-movement.service';

@Component({
  selector: 'app-stock-movement-list',
  template: `
    <div role="main" class="main-content">
      <div class="page-content container container-plus">
        <div class="card-header">
          <h3 class="card-title text-125 text-primary-d2">
            <i [ngClass]="kind === 'entry' ? 'fa fa-plus-circle' : 'fa fa-minus-circle'"
               class="text-dark-l3 mr-1"></i>
            {{ title }}
          </h3>
        </div>

        <div class="row">
          <div class="col-12">
            <div class="card dcard">
              <div class="card-body px-1 px-md-3">
                <div autocomplete="off">
                  <div class="d-flex justify-content-between flex-column flex-sm-row mb-3 px-2 px-sm-0">
                    <div class="mb-2 mb-sm-0">
                      <a class="btn px-4 btn-primary mb-1" [href]="createUrl">
                        {{ kind === 'entry' ? 'Nueva entrada' : 'Nuevo retiro' }}
                      </a>
                    </div>
                  </div>

                  <div class="row mb-3">
                    <div class="col-md-3 mb-2">
                      <input type="text" class="form-control" placeholder="Codigo u observacion"
                             [(ngModel)]="searchFilter.Query" (keyup.enter)="filterPage(1)">
                    </div>
                    <div class="col-md-2 mb-2">
                      <select class="form-control" [(ngModel)]="searchFilter.ProcessStatus">
                        <option value="">Todos los estados</option>
                        <option value="P">Pendiente</option>
                        <option value="C">Confirmado</option>
                        <option value="R">Rechazado</option>
                        <option value="X">Anulado</option>
                      </select>
                    </div>
                    <div class="col-md-3 mb-2">
                      <input type="date" class="form-control" [(ngModel)]="searchFilter.DateStart">
                    </div>
                    <div class="col-md-4 mb-2">
                      <input type="date" class="form-control" [(ngModel)]="searchFilter.DateEnd">
                    </div>
                  </div>

                  <div class="mb-3">
                    <button class="btn px-4 btn-primary" (click)="filterPage(1)">Buscar</button>
                  </div>

                  <app-table [data]="dataTablaGenetic" [action]="this"></app-table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <app-modalconfirm
      [id]="'modal_reject_stock_movement'"
      [title]="'Rechazar movimiento de stock'"
      [message]="'El documento pendiente quedara rechazado. ¿Desea continuar?'"
      [action]="this">
    </app-modalconfirm>

    <app-modalconfirm
      [id]="'modal_cancel_stock_movement'"
      [title]="'Anular movimiento de stock'"
      [message]="'El documento pendiente quedara anulado. ¿Desea continuar?'"
      [action]="this">
    </app-modalconfirm>
  `
})
export class StockMovementListComponent implements OnInit, ActionTableService<any>, ActionModalConfirmService {
  @Input() kind: StockMovementKind = 'entry';
  responsePageSearch: ResponsePageSearch<any> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<any> = new DataTablaGeneticDto();
  movementSelect: any = null;
  searchFilter: any = { Query: '', ProcessStatus: '', ProcessType: 'O', DateStart: null, DateEnd: null, Page: 1 };

  constructor(private service: StockMovementService, private toastr: ToastrService) {}

  ngOnInit(): void { void this.findAll(1, ''); }
  get title(): string { return this.kind === 'entry' ? 'Entradas excepcionales de stock' : 'Retiros excepcionales de stock'; }
  get createUrl(): string { return `enterprise/inventory/pages/create${this.kind === 'entry' ? 'stockentry' : 'stockexit'}`; }
  code(row: any): string { return row.StockEntryCod || row.StockExitCod; }

  filterPage(page: number): void {
    void this.findAll(page, this.searchFilter.Query);
  }

  filter(page: number): void {
    this.filterPage(page);
  }

  async findAll(page: number, query: string = ''): Promise<void> {
    this.searchFilter.Page = page;
    this.searchFilter.Query = query ?? this.searchFilter.Query;
    const response = await this.service.findAll(this.kind, this.searchFilter);
    if (response.ErrorStatus) { this.toastr.error(response.Message); return; }
    this.responsePageSearch = response.Data;
    this.loadingTable(this.responsePageSearch);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<any>): void {
    const data = new DataTablaGeneticDto<any>();
    const showPending = (row: any) => row.ProcessStatus === 'P';
    const showResolution = (row: any) =>
      row.ProcessStatus === 'C' && row.MovementMode === 'N' && row.HasPendingResolution === true;

    data.init(
      [
        { Name: 'Codigo', key: 'Code', FunctionKey: (row: any) => this.code(row) },
        {
          Name: 'Modalidad',
          key: 'MovementMode',
          IsStatus: true,
          Html: {
            D: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            N: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25'
          },
          Mask: { D: 'Directo', N: 'No disponible' }
        },
        {
          Name: 'Estado',
          key: 'ProcessStatus',
          IsStatus: true,
          Html: {
            P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25',
            C: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            R: 'badge badge-sm bgc-dark text-white pb-1 px-25',
            X: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask: { P: 'Pendiente', C: 'Confirmado', R: 'Rechazado', X: 'Anulado' }
        },
        { Name: 'Creacion', key: 'CreationDate', IsDate: true },
        {
          Name: 'Opciones',
          ColumnAction: true,
          Id: [this.kind === 'entry' ? 'StockEntryCod' : 'StockExitCod'],
          Options: [
            {
              Type: 'Url', Name: 'fa fa-eye', Title: 'Ver movimiento',
              FunctionUrl: (row: any) => this.url('view', row)
            },
            {
              Type: 'Url', Name: 'fa fa-edit', Title: 'Editar movimiento',
              Function: showPending, FunctionUrl: (row: any) => this.url('edit', row)
            },
            {
              Type: 'Url', Name: 'fa fa-gavel', Title: 'Resolver stock no disponible',
              Function: showResolution, FunctionUrl: (row: any) => this.url('resolve', row)
            },
            {
              Type: 'Modal', Name: 'fa fa-times-circle', Title: 'Rechazar movimiento',
              ID: 'modal_reject_stock_movement', Function: showPending
            },
            {
              Type: 'Modal', Name: 'fa fa-ban', Title: 'Anular movimiento',
              ID: 'modal_cancel_stock_movement', Function: showPending
            }
          ]
        }
      ],
      { data: responsePageSearch },
      this.kind === 'entry' ? 'Bandeja de entradas de stock' : 'Bandeja de retiros de stock'
    );
    this.dataTablaGenetic = data;
  }

  getDataRow(item: any): void {
    this.movementSelect = item;
  }

  private url(action: string, row: any): string {
    const suffix = this.kind === 'entry' ? 'stockentry' : 'stockexit';
    const page = action === 'edit' ? `create${suffix}` : `${action}${suffix}`;
    const parameter = action === 'resolve' ? 'originCode' : 'code';
    return `/enterprise/inventory/pages/${page}?${parameter}=${encodeURIComponent(this.code(row))}`;
  }

  async actionModal(modalId: string): Promise<void> {
    if (!this.movementSelect) return;
    if (modalId === 'modal_cancel_stock_movement') await this.cancelSelected();
    if (modalId === 'modal_reject_stock_movement') await this.rejectSelected();
  }

  private async cancelSelected(): Promise<void> {
    const response = await this.service.cancel(this.kind, this.code(this.movementSelect), 'Anulado desde la bandeja');
    if (response.ErrorStatus) this.toastr.error(response.Message);
    else { this.toastr.success('Movimiento anulado'); await this.findAll(this.searchFilter.Page, this.searchFilter.Query); }
  }

  private async rejectSelected(): Promise<void> {
    const observation = window.prompt('Indique el motivo del rechazo:')?.trim() || '';
    if (!observation) return;
    const response = await this.service.reject(this.kind, this.code(this.movementSelect), observation);
    if (response.ErrorStatus) this.toastr.error(response.Message);
    else { this.toastr.success('Movimiento rechazado'); await this.findAll(this.searchFilter.Page, this.searchFilter.Query); }
  }
}
