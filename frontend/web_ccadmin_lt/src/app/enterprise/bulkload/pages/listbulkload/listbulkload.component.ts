import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { BulkLoadConstants } from '../../model/BulkLoadConstants';
import { BulkLoadHead } from '../../model/BulkLoadModels';
import { BulkLoadService } from '../../service/bulk-load.service';

@Component({
  selector: 'app-listbulkload',
  templateUrl: './listbulkload.component.html',
  styleUrls: ['./listbulkload.component.css']
})
export class ListBulkLoadComponent
  implements OnInit, ActionTableService<BulkLoadHead> {

  readonly constants = BulkLoadConstants;
  responsePageSearch = new ResponsePageSearch<BulkLoadHead>();
  dataTablaGenetic = new DataTablaGeneticDto<BulkLoadHead>();
  bulkLoadSelect: BulkLoadHead | null = null;
  loading = false;
  search = {
    Query: '',
    BulkLoadType: '',
    ProcessStatus: '',
    DateStart: null as string | null,
    DateEnd: null as string | null,
    Page: 1
  };

  constructor(
    private service: BulkLoadService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    void this.findAll(1);
  }

  filter(page: number): void {
    void this.findAll(page);
  }

  async findAll(page: number, query: string = ''): Promise<void> {
    this.loading = true;
    this.search.Page = page;
    if (query) this.search.Query = query;
    try {
      const response = await this.service.findAll(this.search);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.responsePageSearch = response.Data as ResponsePageSearch<BulkLoadHead>;
      this.loadingTable(this.responsePageSearch);
    } finally {
      this.loading = false;
    }
  }

  loadingTable(responsePageSearch: ResponsePageSearch<BulkLoadHead>): void {
    const data = new DataTablaGeneticDto<BulkLoadHead>();
    const showEdit = (item: BulkLoadHead): boolean =>
      BulkLoadConstants.isEditable(item);
    const statusKey = (item: BulkLoadHead): string =>
      item.ProcessStatus === BulkLoadConstants.FINALIZED && item.NumErrorDetails > 0
        ? 'F_ERROR'
        : item.ProcessStatus;

    data.init(
      [
        { Name: 'Código', key: 'BulkLoadCod' },
        {
          Name: 'Tipo',
          key: 'BulkLoadType',
          FunctionKey: (item: BulkLoadHead) =>
            BulkLoadConstants.typeDescription(item.BulkLoadType)
        },
        { Name: 'Archivo', key: 'OriginalFileName' },
        {
          Name: 'Locales',
          key: 'NumDestinations',
          FunctionKey: (item: BulkLoadHead) =>
            BulkLoadConstants.requiresDestinations(item.BulkLoadType)
              ? String(item.NumDestinations) : 'No aplica'
        },
        {
          Name: 'Registros',
          key: 'NumProcessedDetails',
          FunctionKey: (item: BulkLoadHead) =>
            `${item.NumProcessedDetails}/${item.NumTotalDetails}`
        },
        { Name: 'Errores', key: 'NumErrorDetails' },
        {
          Name: 'Progreso',
          key: 'ProgressPercent',
          IsProgress: true,
          FunctionKey: (item: BulkLoadHead) => this.progress(item.ProgressPercent),
          ProgressTextFunction: (item: BulkLoadHead) =>
            `${this.progress(item.ProgressPercent).toFixed(0)}% procesado`,
          ProgressClassFunction: (item: BulkLoadHead) =>
            this.progressClass(item.ProcessStatus)
        },
        {
          Name: 'Estado',
          key: 'ProcessStatus',
          IsStatus: true,
          FunctionKey: statusKey,
          Html: {
            D: 'badge badge-sm bgc-secondary text-white pb-1 px-25',
            V: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            P: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
            Q: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            W: 'badge badge-sm bgc-primary text-white pb-1 px-25',
            F: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            F_ERROR: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
            E: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25',
            X: 'badge badge-sm bgc-secondary text-white pb-1 px-25',
            C: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25'
          },
          Mask: {
            D: 'Borrador',
            V: 'Validando',
            P: 'Pendiente',
            Q: 'En cola',
            W: 'Procesando',
            F: 'Finalizado',
            F_ERROR: 'Finalizado con errores',
            E: 'Error',
            X: 'Anulado',
            C: 'Confirmado'
          }
        },
        { Name: 'Creación', key: 'CreationDate', IsDate: true },
        {
          Name: 'Opciones',
          ColumnAction: true,
          Id: ['BulkLoadCod'],
          Options: [
            {
              Type: 'Url',
              Name: 'fa fa-eye',
              Title: 'Ver carga masiva',
              Url: '/enterprise/bulkload/pages/viewbulkload?BulkLoadCod={BulkLoadCod}'
            },
            {
              Type: 'Url',
              Name: 'fa fa-edit',
              Title: 'Editar o corregir carga',
              Url: '/enterprise/bulkload/pages/createbulkload?BulkLoadCod={BulkLoadCod}',
              Function: showEdit
            }
          ]
        }
      ],
      { data: responsePageSearch },
      'Bandeja de cargas masivas'
    );

    this.dataTablaGenetic = data;
  }

  getDataRow(item: BulkLoadHead): void {
    this.bulkLoadSelect = item;
  }

  progress(value: number | null | undefined): number {
    return Math.min(100, Math.max(0, Number(value ?? 0)));
  }

  private progressClass(status: string): string {
    if (status === BulkLoadConstants.ERROR) return 'bgc-red-d1';
    if (status === BulkLoadConstants.FINALIZED
      || status === BulkLoadConstants.CONFIRMED) {
      return 'bgc-success-d1';
    }
    if (status === BulkLoadConstants.CANCELLED) return 'bgc-secondary';
    return 'bgc-primary';
  }
}
