import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { StoreSequenceEntity } from 'src/app/enterprise/shared/model/entity/StoreSequenceEntity';
import { StoreSequenceService } from '../../service/StoreSequenceService';

@Component({
  selector: 'app-liststoresequence',
  templateUrl: './liststoresequence.component.html'
})
export class ListstoresequenceComponent implements OnInit, ActionTableService<StoreSequenceEntity> {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  @ViewChild('cboStore') cboStore!: ElementRef<HTMLSelectElement>;

  responsePageSearch: ResponsePageSearch<StoreSequenceEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<StoreSequenceEntity> = new DataTablaGeneticDto();
  storeList: StoreEntity[] = [];

  constructor(
    private storeSequenceService: StoreSequenceService
  ) { }

  ngOnInit(): void {
    this.loadFilterData();
    this.findAll(1, "");
  }

  async loadFilterData(): Promise<void> {
    const rpt: ResponseWsDto = await this.storeSequenceService.findDataForm();
    if (!rpt.ErrorStatus) {
      this.storeList = rpt.DataAdditional?.find((e: any) => e.Name === 'StoreList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'storeList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'stores')?.Data
        ?? [];
    }
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value, this.cboStore.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<StoreSequenceEntity>): void {
    const data: DataTablaGeneticDto<StoreSequenceEntity> = new DataTablaGeneticDto();

    data.init(
      [
        { Name: "Tienda", key: "StoreCod" },
        { Name: "Periodo", key: "PeriodId" },
        { Name: "Tabla", key: "SequenceTableType" },
        { Name: "Prefijo", key: "Prefix" },
        { Name: "Secuencia", key: "SequenceTrx" },
        { Name: "Longitud", key: "SequenceLength" },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["StoreCod", "PeriodId", "SequenceTableType"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/system/pages/createstoresequence?StoreCod={StoreCod}&PeriodId={PeriodId}&SequenceTableType={SequenceTableType}" }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de secuencias por tienda"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string, StoreCod: string = ""): Promise<void> {
    const rpt: ResponseWsDto = await this.storeSequenceService.findAll(Query, Page, StoreCod);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
  }

  getStoreList(): StoreEntity[] {
    return this.storeList;
  }
}
