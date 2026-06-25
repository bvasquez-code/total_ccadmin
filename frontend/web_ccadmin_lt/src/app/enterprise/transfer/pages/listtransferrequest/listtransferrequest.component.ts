import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { TransferHeadEntity } from '../../model/entity/TransferHeadEntity';
import { TransferService } from '../../service/TransferService';
import { TransferSearchDto } from '../../model/dto/TransferSearchDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { TransferConstants } from '../../model/constants/TransferConstants';
import { TransferRequestHeadEntity } from '../../model/entity/TransferRequestHeadEntity';
import { TransferRequestService } from '../../service/TransferRequestService';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';

@Component({
  selector: 'app-listtransferrequest',
  templateUrl: './listtransferrequest.component.html'
})
export class ListtransferrequestComponent implements OnInit, ActionTableService<TransferRequestHeadEntity> {

  @ViewChild('txtTransferCod') txtTransferCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDateStart') txtDateStart!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDateEnd') txtDateEnd!: ElementRef<HTMLInputElement>;
  @ViewChild('cboStoreOrigin') cboStoreOrigin!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboStoreDest') cboStoreDest!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboStatus') cboStatus!: ElementRef<HTMLSelectElement>;

  responsePageSearch: ResponsePageSearch<TransferRequestHeadEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<TransferRequestHeadEntity> = new DataTablaGeneticDto();
  transferHeadSelect: TransferRequestHeadEntity = new TransferRequestHeadEntity();
  storeList: StoreEntity[] = [];

  statusList = [
    { Code: '', Name: 'Todos' },
    { Code: 'P', Name: 'Pendiente' },
    { Code: 'C', Name: 'Confirmada' },
    { Code: 'R', Name: 'Rechazada' },
    { Code: 'X', Name: 'Anulada' },
    { Code: 'A', Name: 'Aprobada' }
  ];

  constructor(
    private transferRequestService: TransferRequestService,
    private sessionService: DataSesionService
  ) { }

  ngOnInit(): void {
    this.loadFilterData();
    this.findAll(1);
  }

  async loadFilterData() {
    const rpt: ResponseWsDto = await this.transferRequestService.FindDataForm('');
    if (!rpt.ErrorStatus) {
      const storeList = rpt.DataAdditional?.find((e: any) => e.Name === 'StoreList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'storeList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'stores')?.Data
        ?? [];
      this.storeList = storeList;
    }
  }

  filter(Page: number): void {
    this.findAll(Page);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<TransferRequestHeadEntity>): void {
    const data: DataTablaGeneticDto<TransferRequestHeadEntity> = new DataTablaGeneticDto();

    const showReceive = (transferHead: TransferRequestHeadEntity) => {
      return transferHead.TransferStatus === 'A';
    };

    const hasDocument = (transferHead: any) => {
      if (transferHead.DocumentCod) return 'Sí';
      if (transferHead.HasDocument !== undefined) return transferHead.HasDocument ? 'Sí' : 'No';
      if (transferHead.transferDocumentList) return transferHead.transferDocumentList.length > 0 ? 'Sí' : 'No';
      return 'No';
    };

    data.init(
      [
        { Name: 'Código', key: 'TransferReqCod' },
        { Name: 'Local origen', key: 'StoreCodOrigin' },
        { Name: 'Local destino', key: 'StoreCodDest' },
        { Name: 'Documento', key: 'TransferReqCod', FunctionKey: hasDocument },
        {
          Name: 'Estado',
          key: 'TransferStatus',
          IsStatus: true,
          Html: {
            P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25',
            C: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            D: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
            F: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            R: 'badge badge-sm bgc-dark text-white pb-1 px-25',
            X: 'badge badge-sm bgc-secondary text-white pb-1 px-25',
            A: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask: {
            P: 'Pendiente',
            C: 'Confirmada',
            D: 'Despachada',
            F: 'Finalizada',
            R: 'Rechazada',
            X: 'Anulada',
            A: 'Aprobada'
          }
        },
        { Name: 'Creación', key: 'CreationDate', IsDate: true },
        {
          Name: 'Opciones',
          ColumnAction: true,
          Id: ['TransferReqCod'],
          Options: [
            { Type: 'Url', Name: 'fa fa-eye', Url: '/enterprise/transfer/pages/transferdetail?TransferCod={TransferReqCod}' },
            { Type: 'Url', Name: 'fa fa-check', Url: '/enterprise/transfer/pages/receivetransfer?TransferReqCod={TransferReqCod}', Function: showReceive }
          ]
        }
      ],
      {
        data: responsePageSearch
      },
      'Bandeja de solicitudes de transferencia'
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number): Promise<void> {
    const search: TransferSearchDto = new TransferSearchDto();
    search.Page = Page;
    search.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
    search.TransferCod = this.txtTransferCod?.nativeElement.value ?? '';
    search.StoreCodOrigin = this.cboStoreOrigin?.nativeElement.value ?? '';
    search.StoreCodDest = this.sessionService.getSessionStorageDto().StoreCod;
    search.TransferStatus = this.cboStatus?.nativeElement.value ?? '';
    search.DateStart = this.txtDateStart?.nativeElement.value ?? '';
    search.DateEnd = this.txtDateEnd?.nativeElement.value ?? '';

    const rpt: ResponseWsDto = await this.transferRequestService.FindAll(search);
    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.transferHeadSelect = item;
  }

  getStoreList() {
    return this.storeList.filter(e => e.StoreCod !== this.sessionService.getSessionStorageDto().StoreCod);
  }
}
