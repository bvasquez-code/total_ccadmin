import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { TransferHeadEntity } from '../../model/entity/TransferHeadEntity';
import { TransferService } from '../../service/TransferService';
import { TransferSearchDto } from '../../model/dto/TransferSearchDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';

@Component({
  selector: 'app-listtransferdispatch',
  templateUrl: './listtransferdispatch.component.html'
})
export class ListtransferdispatchComponent implements OnInit, ActionTableService<TransferHeadEntity> {

  @ViewChild('txtTransferCod') txtTransferCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDateStart') txtDateStart!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDateEnd') txtDateEnd!: ElementRef<HTMLInputElement>;
  @ViewChild('cboStoreOrigin') cboStoreOrigin!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboStatus') cboStatus!: ElementRef<HTMLSelectElement>;

  responsePageSearch: ResponsePageSearch<TransferHeadEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<TransferHeadEntity> = new DataTablaGeneticDto();
  transferHeadSelect: TransferHeadEntity = new TransferHeadEntity();
  storeList: StoreEntity[] = [];

  statusList = [
    { Code: '', Name: 'Todos' },
    { Code: 'P', Name: 'Pendiente' },
    { Code: 'C', Name: 'Confirmada' },
    { Code: 'D', Name: 'Despachada' },
    { Code: 'F', Name: 'Finalizada' },
    { Code: 'R', Name: 'Rechazada' },
    { Code: 'X', Name: 'Anulada' }
  ];

  constructor(
    private transferService: TransferService,
    private dataSesionService: DataSesionService
  ) { }

  ngOnInit(): void {
    this.loadFilterData();
    this.findAll(1);
  }

  async loadFilterData() {
    const rpt: ResponseWsDto = await this.transferService.FindDataForm('');
    if (!rpt.ErrorStatus) {
      const storeList: StoreEntity[] = rpt.DataAdditional?.find((e: any) => e.Name === 'storeList')?.Data ?? [];
      this.storeList = storeList.filter(e => e.StoreCod !== this.dataSesionService.getSessionStorageDto().StoreCod);
    }
  }

  filter(Page: number): void {
    this.findAll(Page);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<TransferHeadEntity>): void {
    const data: DataTablaGeneticDto<TransferHeadEntity> = new DataTablaGeneticDto();

    const showDispatch = (transferHead: TransferHeadEntity) => {
      return transferHead.TransferStatus === 'P';
    };

    data.init(
      [
        { Name: 'Código', key: 'TransferCod' },
        { Name: 'Solicitante', key: 'StoreCodRequestedBy' },
        { Name: 'Origen', key: 'StoreCodOrigin' },
        { Name: 'Destino', key: 'StoreCodDest' },
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
            X: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask: {
            P: 'Pendiente',
            C: 'Confirmada',
            D: 'Despachada',
            F: 'Finalizada',
            R: 'Rechazada',
            X: 'Anulada'
          }
        },
        { Name: 'Creación', key: 'CreationDate', IsDate: true },
        {
          Name: 'Opciones',
          ColumnAction: true,
          Id: ['TransferCod'],
          Options: [
            { Type: 'Url', Name: 'fa fa-eye', Url: '/enterprise/transfer/pages/transferdetail?TransferCod={TransferCod}' },
            { Type: 'Url', Name: 'fa fa-truck', Url: '/enterprise/transfer/pages/dispatchtransfer?TransferCod={TransferCod}', Function: showDispatch }
          ]
        }
      ],
      {
        data: responsePageSearch
      },
      'Bandeja de despacho de transferencias'
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number): Promise<void> {
    const search: TransferSearchDto = new TransferSearchDto();
    search.Page = Page;
    search.TypeOperation = 'TS';
    search.TransferCod = this.txtTransferCod?.nativeElement.value ?? '';
    search.StoreCodDest = this.cboStoreOrigin?.nativeElement.value ?? '';
    search.TransferStatus = this.cboStatus?.nativeElement.value ?? '';
    search.DateStart = this.txtDateStart?.nativeElement.value ?? '';
    search.DateEnd = this.txtDateEnd?.nativeElement.value ?? '';
    search.StoreCodOrigin = this.dataSesionService.getSessionStorageDto().StoreCod;

    const rpt: ResponseWsDto = await this.transferService.FindAll(search);
    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.transferHeadSelect = item;
  }
}
